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
import { describe, it, expect, vi, beforeAll, afterEach } from "vitest";
import { fireEvent, waitFor } from "@testing-library/react";
import { Formio } from "@formio/js";
import FormIoUploader from "../../components/formio/FormIoUploader";

const createUploader = (component: object, data: object) =>
  new FormIoUploader(
    { type: "portalFileUpload", key: "bestanden.upload", ...component },
    {},
    data,
  );

describe("FormIoUploader", () => {
  it("is truthy", () => {
    expect(FormIoUploader).toBeTruthy();
  });

  describe("getBeschrijving", () => {
    it("returns the value at beschrijvingPath", () => {
      const uploader = createUploader(
        { beschrijvingPath: "bestanden.beschrijving" },
        { bestanden: { beschrijving: "Kopie identiteitsbewijs" } },
      );

      expect(uploader.getBeschrijving()).toBe("Kopie identiteitsbewijs");
    });

    it("returns undefined when beschrijvingPath is not set", () => {
      const uploader = createUploader(
        {},
        { bestanden: { beschrijving: "Kopie identiteitsbewijs" } },
      );

      expect(uploader.getBeschrijving()).toBeUndefined();
    });

    it("returns undefined when the path does not resolve", () => {
      const uploader = createUploader(
        { beschrijvingPath: "bestanden.onbekend" },
        { bestanden: {} },
      );

      expect(uploader.getBeschrijving()).toBeUndefined();
    });

    it.each([
      ["an empty string", ""],
      ["whitespace", "   "],
      ["a number", 42],
      ["an object", { tekst: "Kopie" }],
    ])("returns undefined when the value is %s", (_, value) => {
      const uploader = createUploader(
        { beschrijvingPath: "bestanden.beschrijving" },
        { bestanden: { beschrijving: value } },
      );

      expect(uploader.getBeschrijving()).toBeUndefined();
    });
  });

  describe("upload", () => {
    const fetchMock = vi.fn();

    beforeAll(() => {
      FormIoUploader.register();
      sessionStorage.setItem("REST_URI", "http://localhost/api");
    });

    afterEach(() => {
      fetchMock.mockReset();
      vi.unstubAllGlobals();
      document.body.innerHTML = "";
    });

    const uploadFile = async (
      uploaderComponent: object,
      response = new Response(JSON.stringify({ url: "http://document" })),
    ) => {
      fetchMock.mockResolvedValue(response);
      vi.stubGlobal("fetch", fetchMock);

      const element = document.createElement("div");
      document.body.appendChild(element);

      await Formio.createForm(
        element,
        {
          components: [
            {
              key: "bestanden.beschrijving",
              type: "hidden",
              input: true,
              defaultValue: "Kopie identiteitsbewijs partner",
            },
            {
              key: "bestanden.partnerID",
              type: "portalFileUpload",
              input: true,
              label: "Identiteitsbewijs",
              ...uploaderComponent,
            },
          ],
        },
        { taakId: "taak-id" },
      );

      const input = await waitFor(() => {
        const fileInput =
          element.querySelector<HTMLInputElement>('input[type="file"]');
        expect(fileInput).not.toBeNull();
        return fileInput!;
      });

      fireEvent.change(input, {
        target: { files: [new File(["inhoud"], "id.pdf")] },
      });

      await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));

      const [url, init] = fetchMock.mock.calls[0];
      return { element, url, body: init.body as FormData };
    };

    it("sends the value of the hidden field as beschrijving", async () => {
      const { url, body } = await uploadFile({
        beschrijvingPath: "bestanden.beschrijving",
      });

      expect(url).toBe("http://localhost/api/taak/taak-id/document/content");
      expect(body.get("beschrijving")).toBe("Kopie identiteitsbewijs partner");
    });

    it("does not send beschrijving without beschrijvingPath", async () => {
      const { body } = await uploadFile({});

      expect(body.has("file")).toBe(true);
      expect(body.has("beschrijving")).toBe(false);
    });

    it("shows a message when the beschrijving is too long", async () => {
      const { element } = await uploadFile(
        { beschrijvingPath: "bestanden.beschrijving" },
        new Response(JSON.stringify({ error: "BESCHRIJVING_TOO_LONG" }), {
          status: 400,
        }),
      );

      await waitFor(() =>
        expect(element.textContent).toContain(
          "De beschrijving van het document is te lang. Neem contact op met de gemeente.",
        ),
      );
    });

    it("shows the generic error for other failed uploads", async () => {
      const { element } = await uploadFile(
        {},
        new Response("Internal Server Error", { status: 500 }),
      );

      await waitFor(() =>
        expect(element.textContent).toContain(
          "Er is een fout opgetreden tijdens het uploaden van het bestand.",
        ),
      );
      expect(element.textContent).not.toContain(
        "De beschrijving van het document is te lang. Neem contact op met de gemeente.",
      );
    });
  });
});
