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
import { type ContextType, type ReactNode } from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { createIntl, IntlProvider } from "react-intl";
import { MemoryRouter, Route, Routes } from "react-router";
import { shortDateOptions } from "@gemeente-denhaag/utils";
import { renderTemplate } from "../../utils/liquid/renderTemplate";
import LiquidParser from "../../components/LiquidParser";
import { LIQUID_FORMATTERS } from "../../utils/liquid/formatters";
import PortalPageBindingContext, {
  type PortalPageBindingContextValue,
} from "../../contexts/PortalPageBindingContext";
import { NotificationProvider } from "../../contexts/NotificationContext";
import AppContext from "../../contexts/AppContext";

const intl = createIntl({ locale: "nl", onError: () => {} });

const renderNodes = async (
  template: string,
  data: Record<string, unknown> = {},
  binding?: PortalPageBindingContextValue,
) => {
  const nodes = await renderTemplate(template, data, intl);
  const wrap = (children: ReactNode) => (
    <IntlProvider locale="nl" onError={() => {}}>
      <MemoryRouter>
        <PortalPageBindingContext.Provider value={binding}>
          {children}
        </PortalPageBindingContext.Provider>
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
Naam | {{ naam }}
Status | {{ status | capitalize }}
Prijs | {{ prijs | currency }}
{% enddescription-list %}

{% table title: "Dit is een tabel" %}
Omschrijving | Bedrag
**[Aanvraag](/parkeren/{{ uuid }})** | {{ prijs | currency }}
Wijzigen | [{{ 10 | currency }}](/parkeren/{{ uuid }}/wijzigen)
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

  it("formats currency, shows escape entities as text and template text as written", async () => {
    await renderNodes(
      `{% paragraph %}{{ prijs | currency }}{% endparagraph %}{% paragraph %}{{ naam | escape }}{% endparagraph %}{% paragraph %}Noord &amp; Zuid{% endparagraph %}`,
      product,
    );

    expect(document.body.textContent).toContain(
      LIQUID_FORMATTERS.currency(intl, 12.5),
    );
    expect(
      screen.getByText("Erfpacht &#34;Noord&#34; &amp; &lt;Zuid&gt;"),
    ).toBeInTheDocument();
    expect(screen.getByText("Noord &amp; Zuid")).toBeInTheDocument();
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
      `{% heading level: 2 %}Over {{ naam }}{% endheading %}
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
      `{% image src: "mailto:a@example.org" %}`,
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

  it("renders a link list without the default title", async () => {
    await renderNodes(`{% link-list %}
Mijn zaken | /zaken
{% endlink-list %}`);

    expect(screen.queryByRole("heading")).toBeNull();
  });

  it("opens a _blank link in a new tab and rejects another target", async () => {
    await renderNodes(
      `{% link href: "https://www.denhaag.nl" title: "Den Haag" target: "_blank" %}`,
    );

    const link = screen.getByRole("link", { name: "Den Haag" });
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
    await expect(
      renderTemplate(
        `{% link href: "/zaken" title: "x" target: "_parent" %}`,
        {},
        intl,
      ),
    ).rejects.toThrow("Link target must be _self or _blank.");
  });

  it("navigates an internal link through the router", async () => {
    const nodes = await renderTemplate(
      `{% link href: "/zaken" title: "Mijn zaken" %}`,
      {},
      intl,
    );
    render(
      <IntlProvider locale="nl" onError={() => {}}>
        <MemoryRouter initialEntries={["/"]}>
          <Routes>
            <Route path="/" element={<>{nodes}</>} />
            <Route path="/zaken" element={<p>Zakenpagina</p>} />
          </Routes>
        </MemoryRouter>
      </IntlProvider>,
    );

    fireEvent.click(screen.getByRole("link", { name: "Mijn zaken" }));

    expect(screen.getByText("Zakenpagina")).toBeInTheDocument();
  });

  describe("back-link", () => {
    const renderBackLink = async (template: string, history: string[]) => {
      const nodes = await renderTemplate(template, {}, intl);
      render(
        <IntlProvider
          locale="nl"
          messages={{ "backlink.back": "Terug", naarlijst: "Naar de lijst" }}
          onError={() => {}}
        >
          <MemoryRouter>
            <AppContext.Provider
              value={{ history } as unknown as ContextType<typeof AppContext>}
            >
              {nodes}
            </AppContext.Provider>
          </MemoryRouter>
        </IntlProvider>,
      );
    };

    it("links to href with the standard text", async () => {
      await renderBackLink(`{% back-link href: "/mijn-producten" %}`, []);

      expect(screen.getByRole("link", { name: "Terug" })).toHaveAttribute(
        "href",
        "/mijn-producten",
      );
    });

    it("uses title as a message id and otherwise as text", async () => {
      await renderBackLink(
        `{% back-link href: "/a" title: "naarlijst" %}{% back-link href: "/b" title: "Naar mijn producten" %}`,
        [],
      );

      expect(
        screen.getByRole("link", { name: "Naar de lijst" }),
      ).toHaveAttribute("href", "/a");
      expect(
        screen.getByRole("link", { name: "Naar mijn producten" }),
      ).toHaveAttribute("href", "/b");
    });

    it("goes back to the previous page without href, and shows nothing without one", async () => {
      await renderBackLink(`{% back-link %}`, ["/huidig", "/vorige"]);
      expect(screen.getByRole("link", { name: "Terug" })).toHaveAttribute(
        "href",
        "/vorige",
      );

      document.body.innerHTML = "";
      await renderBackLink(`{% back-link %}`, ["/huidig"]);
      expect(screen.queryByRole("link")).not.toBeInTheDocument();
    });

    it("rejects a data href that the browser would turn into another site", async () => {
      for (const href of [
        "/\t/evil.example",
        "/\n\\evil.example",
        " /a",
        "/a\u0000",
      ]) {
        await expect(
          renderTemplate(`{% back-link href: href %}`, { href }, intl),
          JSON.stringify(href),
        ).rejects.toThrow("Back link href must be an app path.");
      }
    });

    it("rejects an href that is no app path", async () => {
      for (const href of [
        "https://example.org",
        "//example.org",
        "/\\\\example.org",
        "mijn-producten",
        "javascript:alert(1)",
      ]) {
        await expect(
          renderTemplate(`{% back-link href: "${href}" %}`, {}, intl),
          href,
        ).rejects.toThrow("Back link href must be an app path.");
      }
    });
  });

  it("renders a page header with a message id and with literal text", async () => {
    const nodes = await renderTemplate(
      `{% page-header title: "titel" sub-title: "Uw producten" %}{% endpage-header %}`,
      {},
      intl,
    );
    render(
      <IntlProvider
        locale="nl"
        onError={() => {}}
        messages={{ titel: "Wonen en bouwen" }}
      >
        <MemoryRouter>
          <NotificationProvider>{nodes}</NotificationProvider>
        </MemoryRouter>
      </IntlProvider>,
    );

    expect(
      screen.getByRole("heading", { name: "Wonen en bouwen" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Uw producten" }),
    ).toBeInTheDocument();
  });

  it("renders rich text with a nested link, and an unordered list per line", async () => {
    await renderNodes(
      `{% rich-text %}{% paragraph %}Kijk op {% link href: "/erfpacht" title: "erfpacht" %} voor meer.{% endparagraph %}{% endrich-text %}
{% unordered-list %}
een
twee
{% endunordered-list %}`,
    );

    const link = screen.getByRole("link", { name: "erfpacht" });
    expect(link.closest("p")).toHaveTextContent("Kijk op erfpacht voor meer.");
    expect(screen.getAllByRole("listitem").map((li) => li.textContent)).toEqual(
      ["een", "twee"],
    );
  });

  it("never fails the template for a theme widget's arguments; the query checks them", async () => {
    for (const template of [
      `{% theme-cases %}`,
      `{% theme-tasks theme: "" %}`,
      `{% theme-cases theme: "Wonen" page-size: 0 %}`,
      `{% theme-tasks theme: "Wonen" page-size: 101 %}`,
    ]) {
      await expect(
        renderTemplate(template, {}, intl),
        template,
      ).resolves.toBeDefined();
    }
  });

  describe("output is text", () => {
    const hostile =
      'Jan\n"de" <b>Vries</b> | x \\| y [x](javascript:alert(1)) **kop** &amp; \\';

    it.each([
      [
        "description-list",
        `{% description-list %}\nNaam | {{ v }}\n{% enddescription-list %}`,
      ],
      ["table cell", `{% table %}\nA\n{{ v }}\n{% endtable %}`],
      ["table link label", `{% table %}\nA\n[{{ v }}](/x)\n{% endtable %}`],
      ["link-list", `{% link-list %}\n{{ v }} | /x\n{% endlink-list %}`],
      [
        "unordered-list",
        `{% unordered-list %}\n{{ v }}\n{% endunordered-list %}`,
      ],
      ["heading", `{% heading level: 2 %}{{ v }}{% endheading %}`],
      ["alert", `{% alert %}{{ v }}{% endalert %}`],
      ["paragraph", `{% paragraph %}{{ v }}{% endparagraph %}`],
      ["top level", `{{ v }}`],
    ])(
      "renders a hostile value as one literal value in %s",
      async (_, template) => {
        await renderNodes(template, { v: hostile });

        expect(document.body).toHaveTextContent(hostile.replace(/\s+/g, " "), {
          normalizeWhitespace: true,
        });
        expect(document.querySelectorAll("li, tr").length).toBeLessThanOrEqual(
          2,
        );
        expect(document.querySelectorAll("b, br, th + th")).toHaveLength(0);
        expect(
          [...document.querySelectorAll("a")].every(
            (a) => !a.getAttribute("href")?.startsWith("javascript"),
          ),
        ).toBe(true);
      },
    );

    it.each([
      [
        "filter",
        `{% filter binding: "b" param: "p" title: "F" %}\n{{ v }} | {{ v }}\n{% endfilter %}`,
        "option",
      ],
      [
        "tabs",
        `{% tabs binding: "b" param: "p" %}\n{{ v }} | {{ v }}\n{% endtabs %}`,
        "tab",
      ],
    ])(
      "renders a hostile value as one option in %s",
      async (_, template, role) => {
        await renderNodes(
          template,
          { v: hostile },
          {
            meta: () => ({ params: {} }),
            refetch: vi.fn(),
          },
        );

        expect(screen.getAllByRole(role)).toHaveLength(1);
        expect(screen.getByRole(role).textContent).toBe(hostile);
      },
    );

    it("renders the IKO mock name on one description-list row", async () => {
      const naam =
        'Jan "Jantje" <b>de</b> Vries &amp; Zn.\nTweede regel \\ met backslash';
      await renderNodes(
        `{% description-list %}\nNaam | {{ personen.naam }}\nGeboren | 1980\n{% enddescription-list %}`,
        { personen: { naam } },
      );

      expect(
        screen.getByText(naam, { normalizer: (t) => t }),
      ).toBeInTheDocument();
      expect(screen.getByText("1980")).toBeInTheDocument();
    });

    it("rejects a data href with a disallowed protocol after the values are filled in", async () => {
      await expect(
        renderTemplate(
          `{% table %}\nA\n[x]({{ url }})\n{% endtable %}`,
          { url: "javascript:alert(1)" },
          intl,
        ),
      ).rejects.toThrow("Unsupported table link protocol.");
      await expect(
        renderTemplate(
          `{% link-list %}\nx | {{ url }}\n{% endlink-list %}`,
          { url: "javascript:alert(1)" },
          intl,
        ),
      ).rejects.toThrow();
    });

    it("rejects data hrefs that look relative but leave the portal", async () => {
      for (const url of [
        "//evil.example",
        "/\\evil.example",
        " javascript:alert(1)",
        "java\tscript:alert(1)",
      ]) {
        await expect(
          renderTemplate(
            `{% table %}\nA\n[x]({{ url }})\n{% endtable %}`,
            { url },
            intl,
          ),
          url,
        ).rejects.toThrow("Unsupported table link protocol.");
        await expect(
          renderTemplate(
            `{% link-list %}\nx | {{ url }}\n{% endlink-list %}`,
            { url },
            intl,
          ),
          url,
        ).rejects.toThrow();
      }
      await expect(
        renderTemplate(`{% link href: "//example.org" title: "x" %}`, {}, intl),
      ).rejects.toThrow();
    });

    it("rejects a data image src that the browser would turn into another site", async () => {
      for (const src of [
        "/\\evil.com",
        "\\\\evil.com",
        " //evil.com",
        "\t//evil.com",
        "/\t/evil.com",
      ]) {
        await expect(
          renderTemplate(`{% image src: src %}`, { src }, intl),
          JSON.stringify(src),
        ).rejects.toThrow("Image src must be relative or use https.");
      }
      await expect(
        renderTemplate(`{% image src: src %}`, { src: "/logo.svg" }, intl),
      ).resolves.toBeDefined();
    });

    it("makes raw output part of the template", async () => {
      await renderNodes(`{% table %}\nA | B\n{{ row | raw }}\n{% endtable %}`, {
        row: "a | b",
      });

      expect(screen.getByText("a")).toBeInTheDocument();
      expect(screen.getByText("b")).toBeInTheDocument();
    });

    it("makes echo output part of the template, unlike {{ }}", async () => {
      await renderNodes(`{% table %}\nA | B\n{% echo row %}\n{% endtable %}`, {
        row: "a | b",
      });

      expect(screen.getByText("a")).toBeInTheDocument();
      expect(screen.getByText("b")).toBeInTheDocument();
    });

    it("can't forge a placeholder to show another value", async () => {
      await renderNodes(
        `{% paragraph %}{{ geheim }}{% endparagraph %}{% paragraph %}[{{ v }}]{% endparagraph %}`,
        { geheim: "geheim", v: "\uE0000\uE001" },
      );

      expect(screen.getAllByText("geheim")).toHaveLength(1);
      expect(screen.getByText("[0]")).toBeInTheDocument();
    });

    it("html_text removes tags, decodes entities and stays one value", async () => {
      await renderNodes(
        `{% paragraph %}{{ html | html_text }}{% endparagraph %}{% table %}\nA\n{{ pipe | html_text }}\n{% endtable %}{% paragraph %}[{{ leeg | html_text }}]{% endparagraph %}`,
        {
          html: "<p>Caf&eacute; &amp; caf&#233;</p>",
          pipe: "a &#124; b",
          leeg: null,
        },
      );

      expect(screen.getByText("Café & café")).toBeInTheDocument();
      expect(screen.getByText("a | b").closest("td")).not.toBeNull();
      expect(screen.getByText("[]")).toBeInTheDocument();
    });

    it("lines splits on newlines, trims and gives one break between lines", async () => {
      const { container } = await renderNodes(
        `{% paragraph %}{% assign regels = adres | lines %}{% for regel in regels %}{{ regel }}
{% endfor %}{% endparagraph %}{% assign geen = leeg | lines %}{% paragraph %}[{{ geen.size }}]{% endparagraph %}`,
        { adres: " Straat 1 | [x](y)\r\n2511 AA  Den Haag ", leeg: null },
      );

      const paragraph = container.querySelector("p")!;
      expect(paragraph.querySelectorAll("br")).toHaveLength(1);
      expect(paragraph).toHaveTextContent("Straat 1 | [x](y)2511 AA Den Haag");
      expect(screen.getByText("[0]")).toBeInTheDocument();
    });

    it("lines gives one unordered-list item per line", async () => {
      await renderNodes(
        `{% unordered-list %}{% assign regels = v | lines %}{% for regel in regels %}
{{ regel }}{% endfor %}
{% endunordered-list %}`,
        { v: "een\ntwee | drie" },
      );

      expect(
        screen.getAllByRole("listitem").map((li) => li.textContent),
      ).toEqual(["een", "twee | drie"]);
    });

    it("a template newline in a paragraph is a break, a newline in a value is not", async () => {
      const { container } = await renderNodes(
        `{% paragraph %}een
twee {{ v }}{% endparagraph %}`,
        { v: "drie\nvier" },
      );

      expect(container.querySelectorAll("br")).toHaveLength(1);
      expect(container.querySelector("p")?.textContent).toBe(
        "eentwee drie\nvier",
      );
    });
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
