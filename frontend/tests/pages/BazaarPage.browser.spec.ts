import BazaarPage from "@/pages/BazaarPage.vue"
import { describe, it, expect } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"

describe("bazaar page", () => {
  it("fetch API to be called ONCE on mount", async () => {
    const notebook = makeMe.aNotebook.please()
    const bazaarNotebooks = makeMe.bazaarNotebooks.notebooks(notebook).please()
    const bazaarSpy = mockSdkService("bazaar", bazaarNotebooks)
    helper.component(BazaarPage).withRouter().render()
    expect(bazaarSpy).toBeCalledTimes(1)
  })
})
