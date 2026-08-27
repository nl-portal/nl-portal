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
import { MockTasksPage } from "../mock/pages/TasksPage.mock";
import { describe, it, expect } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { testPaths as paths } from "../../providers/TestProvider";

describe("TasksPage", () => {
  const takenAlgemeneInformatie = () => screen.getByText("OPEN TAAK 1");
  const taakAanvullendeInformatie = () => screen.getByText("OPEN TAAK 4");

  it("Shows an overview of all tasks", async () => {
    render(MockTasksPage());

    await waitFor(async () => {
      expect(takenAlgemeneInformatie()).toBeVisible();
    });

    expect(takenAlgemeneInformatie()).toBeVisible();
    expect(taakAanvullendeInformatie()).toBeVisible();
  });

  it("Allows me to continue to a task", async () => {
    render(MockTasksPage());

    await waitFor(async () => {
      expect(taakAanvullendeInformatie()).toBeVisible();
    });

    expect(screen.getByRole("link", { name: "OPEN TAAK 4" })).toHaveAttribute(
      "href",
      paths.case("66fecaa3-24b4-4739-a7c8-eb58f39e9aae"),
    );
  });
});
