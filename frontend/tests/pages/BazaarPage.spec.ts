import BazaarPage from "@/pages/BazaarPage.vue"
import { describe, it } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("bazaar page", () => {
  it("fetch API to be called ONCE on mount", async () => {
    const notebook = makeMe.aNotebook.please()
    const bazaarNotebooks = makeMe.bazaarNotebooks.notebooks(notebook).please()
    helper.managedApi.restBazaarController.bazaar = vi
      .fn()
      .mockResolvedValue(bazaarNotebooks)
    helper.component(BazaarPage).withRouter().render()
    expect(helper.managedApi.restBazaarController.bazaar).toBeCalledTimes(1)
  })
})
