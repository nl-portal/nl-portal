/*
 * Copyright 2015-2026 Den Haag, Ritense, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { describe, it, expect, vi } from "vitest";
import { type ReactNode } from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { createIntl, IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router";
import { shortDateOptions } from "@gemeente-denhaag/utils";
import { renderTemplate } from "../../utils/liquid/renderTemplate";
import LiquidParser from "../../components/LiquidParser";
import { LIQUID_FORMATTERS } from "../../utils/liquid/formatters";
import PageBindingContext, {
  type PageBindingContextValue,
} from "../../contexts/PageBindingContext";

const intl = createIntl({ locale: "nl", onError: () => {} });

const renderNodes = async (
  template: string,
  data: Record<string, unknown> = {},
  binding?: PageBindingContextValue,
) => {
  const nodes = await renderTemplate(template, data, intl);
  const wrap = (children: ReactNode) => (
    <IntlProvider locale="nl" onError={() => {}}>
      <MemoryRouter>
        <PageBindingContext.Provider value={binding}>
          {children}
        </PageBindingContext.Provider>
      </MemoryRouter>
    </IntlProvider>
  );
  return render(wrap(<>{nodes}</>));
};

const product = {
  uuid: "a1",
  naam: 'Erfpacht "Noord" & <Zuid>',
  startDatum: "2026-01-31",
  status: "actief",
  prijs: 12.5,
};

describe("renderTemplate", () => {
  it("renders the #64 demo template as a DescriptionList and a TableList", async () => {
    await renderNodes(
      `{% description-list title: "Dit is een test" %}
Naam | {{ naam | escape }}
Status | {{ status | capitalize | escape }}
Prijs | {{ prijs | currency | escape }}
{% enddescription-list %}

{% table title: "Dit is een tabel" %}
Omschrijving | Bedrag
**[Aanvraag](/parkeren/{{ uuid }})** | {{ prijs | currency | escape }}
Wijzigen | [{{ 10 | currency | escape }}](/parkeren/{{ uuid }}/wijzigen)
{% endtable %}`,
      product,
    );

    expect(screen.getByText("Dit is een test")).toBeInTheDocument();
    expect(screen.getByText(product.naam)).toBeInTheDocument();
    expect(screen.getByText("Actief")).toBeInTheDocument();
    expect(screen.getByText("Dit is een tabel")).toBeInTheDocument();
    expect(screen.getByText("Aanvraag").closest("a")).toHaveAttribute(
      "href",
      "/parkeren/a1",
    );
  });

  it("parses pipe rows with escaped pipes, bold headers, links and the first link per row", async () => {
    await renderNodes(
      `{% table %}
A | B
**kop** | x \\| y
[eerste](/een) | [tweede](/twee)
{% endtable %}`,
    );

    expect(screen.getByText("x | y")).toBeInTheDocument();
    expect(screen.getByText("kop").closest("th")).not.toBeNull();
    expect(screen.getByText("tweede").closest("a")).toHaveAttribute(
      "href",
      "/een",
    );
  });

  it("rejects a table link with an unsupported protocol and rows of unequal length", async () => {
    await expect(
      renderTemplate(
        `{% table %}\nA\n[x](javascript:alert(1))\n{% endtable %}`,
        {},
        intl,
      ),
    ).rejects.toThrow("Unsupported table link protocol.");
    await expect(
      renderTemplate(`{% table %}\nA | B\nC\n{% endtable %}`, {}, intl),
    ).rejects.toThrow("same number of cells");
  });

  it("formats date as intl short date, or with the built-in strftime argument", async () => {
    await renderNodes(
      `{% paragraph %}{{ startDatum | date }}{% endparagraph %}{% paragraph %}{{ startDatum | date: "%Y/%m/%d" }}{% endparagraph %}`,
      product,
    );

    expect(
      screen.getByText(intl.formatDate(product.startDatum, shortDateOptions)),
    ).toBeInTheDocument();
    expect(screen.getByText("2026/01/31")).toBeInTheDocument();
  });

  it("formats currency and decodes escape once", async () => {
    await renderNodes(
      `{% paragraph %}{{ prijs | currency }}{% endparagraph %}{% paragraph %}{{ naam | escape }}{% endparagraph %}`,
      product,
    );

    expect(document.body.textContent).toContain(
      LIQUID_FORMATTERS.currency(intl, 12.5),
    );
    expect(screen.getByText(product.naam)).toBeInTheDocument();
  });

  it("throws on unknown variables and filters and blocks inherited properties", async () => {
    await expect(renderTemplate("{{ onbekend }}", {}, intl)).rejects.toThrow();
    await expect(
      renderTemplate("{{ naam | onbekend }}", product, intl),
    ).rejects.toThrow();
    await expect(
      renderTemplate("{{ naam.constructor }}", product, intl),
    ).rejects.toThrow();
  });

  it("renders a for loop inside a table body", async () => {
    await renderNodes(
      `{% table %}
Naam | Status
{% for item in items %}{{ item.naam }} | {{ item.status }}
{% endfor %}{% endtable %}`,
      {
        items: [
          { naam: "een", status: "open" },
          { naam: "twee", status: "gesloten" },
        ],
      },
    );

    expect(screen.getByText("een")).toBeInTheDocument();
    expect(screen.getByText("gesloten")).toBeInTheDocument();
  });

  it("renders heading, paragraph, link and image", async () => {
    await renderNodes(
      `{% heading level: 2 %}Over {{ naam | escape }}{% endheading %}
{% paragraph %}Tekst{% endparagraph %}
{% link href: "/zaken" title: "Mijn zaken" %}
{% image src: "https://example.org/logo.svg" alt: "Logo" %}`,
      product,
    );

    expect(
      screen.getByRole("heading", { level: 2, name: `Over ${product.naam}` }),
    ).toBeInTheDocument();
    expect(screen.getByText("Tekst")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Mijn zaken" })).toHaveAttribute(
      "href",
      "/zaken",
    );
    expect(screen.getByAltText("Logo")).toHaveAttribute(
      "src",
      "https://example.org/logo.svg",
    );
  });

  it("rejects a bad image src, link href or heading level", async () => {
    for (const template of [
      `{% image src: "http://example.org/x.png" %}`,
      `{% image src: "javascript:alert(1)" %}`,
      `{% image src: "//example.org/x.png" %}`,
      `{% link href: "javascript:alert(1)" title: "x" %}`,
      `{% heading level: 5 %}x{% endheading %}`,
    ]) {
      await expect(
        renderTemplate(template, {}, intl),
        template,
      ).rejects.toThrow();
    }
  });

  it("table pagination refetches the binding with the page param", async () => {
    const refetch = vi.fn();
    await renderNodes(
      `{% table binding: "producten" page-param: "page" page-size: 2 total: producten.total %}
Naam
{% for p in producten.items %}{{ p }}
{% endfor %}{% endtable %}`,
      { producten: { items: ["a", "b"], total: 5 } },
      { meta: () => ({ params: { page: "0" } }), refetch },
    );

    fireEvent.click(screen.getByRole("button", { name: /volgende|next/i }));

    expect(refetch).toHaveBeenCalledWith("producten", { page: "1" });
  });

  it("table without a binding context renders without pagination", async () => {
    await renderNodes(
      `{% table binding: "producten" page-param: "page" page-size: 2 total: 5 %}
Naam
a
{% endtable %}`,
    );

    expect(screen.queryByRole("navigation")).toBeNull();
  });

  it("filter refetches with the chosen value and resets the page param", async () => {
    const refetch = vi.fn();
    await renderNodes(
      `{% filter binding: "vergunningen" param: "filter.status" title: "Status" page-param: "page" %}
 | Alle
open | Open
{% endfilter %}`,
      {},
      { meta: () => ({ params: { "filter.status": "" } }), refetch },
    );

    fireEvent.change(screen.getByLabelText("Status"), {
      target: { value: "open" },
    });

    expect(refetch).toHaveBeenCalledWith("vergunningen", {
      "filter.status": "open",
      page: "",
    });
  });

  it("renders an alert and rejects an unknown variant", async () => {
    await renderNodes(
      `{% alert variant: "warning" title: "Onderhoud" %}Vanavond niet bereikbaar.{% endalert %}`,
    );

    expect(screen.getByText("Onderhoud")).toBeInTheDocument();
    expect(screen.getByText("Vanavond niet bereikbaar.")).toBeInTheDocument();
    await expect(
      renderTemplate(`{% alert variant: "rood" %}x{% endalert %}`, {}, intl),
    ).rejects.toThrow();
  });

  it("renders a link list with internal and external links and rejects a bad href", async () => {
    await renderNodes(`{% link-list %}
Mijn zaken | /zaken
Den Haag | https://www.denhaag.nl
{% endlink-list %}`);

    expect(screen.getByRole("link", { name: /Mijn zaken/ })).toHaveAttribute(
      "href",
      "/zaken",
    );
    expect(screen.getByRole("link", { name: /Den Haag/ })).toHaveAttribute(
      "href",
      "https://www.denhaag.nl",
    );
    await expect(
      renderTemplate(
        `{% link-list %}x | javascript:alert(1){% endlink-list %}`,
        {},
        intl,
      ),
    ).rejects.toThrow();
    await expect(
      renderTemplate(`{% link-list %}zonder href{% endlink-list %}`, {}, intl),
    ).rejects.toThrow();
  });

  it("tabs select the current value and refetch with the chosen value and reset page", async () => {
    const refetch = vi.fn();
    await renderNodes(
      `{% tabs binding: "producten" param: "filter.status" page-param: "page" %}
 | Alle
actief | Actief
{% endtabs %}`,
      {},
      { meta: () => ({ params: { "filter.status": "actief" } }), refetch },
    );

    expect(screen.getByRole("tab", { name: "Actief" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
    fireEvent.click(screen.getByRole("tab", { name: "Actief" }));
    expect(refetch).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("tab", { name: "Alle" }));
    expect(refetch).toHaveBeenCalledWith("producten", {
      "filter.status": "",
      page: "",
    });
  });

  it("search starts with the current value and refetches with the text and reset page", async () => {
    const refetch = vi.fn();
    await renderNodes(
      `{% search binding: "berichten" param: "filter.q" page-param: "page" title: "Zoek" %}`,
      {},
      { meta: () => ({ params: { "filter.q": "oud" } }), refetch },
    );

    const input = screen.getByLabelText("Zoek");
    expect(input).toHaveValue("oud");
    fireEvent.change(input, { target: { value: " nieuw " } });
    fireEvent.click(screen.getByRole("button"));

    expect(refetch).toHaveBeenCalledWith("berichten", {
      "filter.q": "nieuw",
      page: "",
    });
  });

  it("tabs and search don't react without a binding context", async () => {
    await renderNodes(`{% tabs binding: "x" param: "p" %}
a | A
b | B
{% endtabs %}{% search binding: "x" param: "q" title: "Zoek" %}`);

    expect(screen.getByLabelText("Zoek")).toBeDisabled();
    expect(screen.getByRole("tab", { name: "B" })).toHaveAttribute(
      "tabindex",
      "-1",
    );
  });

  describe("i18n", () => {
    const messagesIntl = createIntl({
      locale: "nl-NL",
      onError: () => {},
      messages: {
        titel: "Producten",
        aantal: "{count, plural, one {# product} other {# producten}}",
        kolom: "Naam",
      },
    });

    it("formats a message with ICU keyword arguments, also inside a table body", async () => {
      const nodes = await renderTemplate(
        `{% paragraph %}{{ 'titel' | i18n }}: {{ 'aantal' | i18n: count: totaal }}{% endparagraph %}{% table %}
{{ 'kolom' | i18n }} | Status
{{ 'titel' | i18n }} | open
{% endtable %}`,
        { totaal: 3 },
        messagesIntl,
      );
      render(
        <IntlProvider locale="nl-NL" onError={() => {}}>
          <MemoryRouter>{nodes}</MemoryRouter>
        </IntlProvider>,
      );

      expect(screen.getByText("Producten: 3 producten")).toBeInTheDocument();
      expect(screen.getByText("Naam")).toBeInTheDocument();
      expect(screen.getAllByText("Producten")).toHaveLength(1);
    });

    it("shows the id of an unknown message", async () => {
      const nodes = await renderTemplate(
        `{% paragraph %}[{{ 'onbekend' | i18n }}]{% endparagraph %}`,
        {},
        messagesIntl,
      );
      render(<>{nodes}</>);

      expect(screen.getByText("[onbekend]")).toBeInTheDocument();
    });

    it("layers page messages over the app messages, also for title arguments", async () => {
      render(
        <IntlProvider
          locale="nl-NL"
          onError={() => {}}
          messages={{
            titel: "App titel",
            algemeen: "Algemeen",
            kop: "App kop",
          }}
        >
          <MemoryRouter>
            <LiquidParser
              template={`{% paragraph %}{{ 'titel' | i18n }} / {{ 'algemeen' | i18n }}{% endparagraph %}{% description-list title: "kop" %}
a | b
{% enddescription-list %}`}
              data={{}}
              messages={{ titel: "Pagina titel", kop: "Pagina kop" }}
            />
          </MemoryRouter>
        </IntlProvider>,
      );

      expect(
        await screen.findByText("Pagina titel / Algemeen"),
      ).toBeInTheDocument();
      expect(screen.getByText("Pagina kop")).toBeInTheDocument();
    });
  });
});
