import { describe, it } from "vitest";
import BazaarPage from "@/pages/BazaarPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("bazaar page", () => {
  beforeEach(() => {
    const teleportTarget = document.createElement("div");
    teleportTarget.id = "head-status";
    document.body.appendChild(teleportTarget);
  });

  it("fetch API to be called ONCE on mount", async () => {
    const notebook = makeMe.aNotebook.please();
    const bazaarNotebooks = makeMe.bazaarNotebooks.notebooks(notebook).please();
    helper.managedApi.restBazaarController.bazaar = vi
      .fn()
      .mockResolvedValue(bazaarNotebooks);
    helper.component(BazaarPage).render();
    expect(helper.managedApi.restBazaarController.bazaar).toBeCalledTimes(1);
  });
});
