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
import { describe, it, expect, beforeAll } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { MockAccountPage } from "../mock/pages/AccountPage.mock";

describe("AccountPage", () => {
  beforeAll(() => {
    window.scrollTo = vi.fn();

    if (!HTMLDialogElement.prototype.showModal) {
      HTMLDialogElement.prototype.showModal = function () {};
    }
    if (!HTMLDialogElement.prototype.close) {
      HTMLDialogElement.prototype.close = function () {};
    }

    Object.defineProperty(window, "matchMedia", {
      writable: true,
      value: vitest.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vitest.fn(), // Deprecated
        removeListener: vitest.fn(), // Deprecated
        addEventListener: vitest.fn(),
        removeEventListener: vitest.fn(),
        dispatchEvent: vitest.fn(),
      })),
    });
  });

  it("should render with all elements present and show double nationality correctly", async () => {
    render(MockAccountPage());

    await waitFor(() => {
      expect(
        screen.getByTestId("persoonsgegevens-firstname"),
      ).toHaveTextContent("Sierra");
    });

    expect(
      screen.getByTestId("persoonsgegevens-nationality"),
    ).toHaveTextContent("Nederlandse, Portugees");
    expect(screen.getByTestId("persoonsgegevens-lastname")).toHaveTextContent(
      "de Kooyman - van der Maassen",
    );
    expect(screen.getByTestId("persoonsgegevens-gender")).toHaveTextContent(
      "vrouw",
    );
    expect(screen.getByTestId("persoonsgegevens-bsn")).toHaveTextContent(
      "999991954",
    );
    expect(screen.getByTestId("persoonsgegevens-birthdate")).toHaveTextContent(
      "3 maart 2003",
    );
    expect(screen.getByTestId("persoonsgegevens-country")).toHaveTextContent(
      "Nederland",
    );
    expect(screen.getByTestId("persoonsgegevens-street")).toHaveTextContent(
      "Leyweg 61e",
    );
    expect(screen.getByTestId("persoonsgegevens-postcode")).toHaveTextContent(
      "2545CC 's-Gravenhage",
    );
  });
});
