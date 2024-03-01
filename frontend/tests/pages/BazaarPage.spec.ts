import { describe, it, beforeEach, afterEach } from "vitest";
import BazaarPage from "@/pages/BazaarPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("bazaar page", () => {
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
