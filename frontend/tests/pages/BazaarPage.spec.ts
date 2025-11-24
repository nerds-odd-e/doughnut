import BazaarPage from "@/pages/BazaarPage.vue"
import { describe, it, expect } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkMethod } from "@tests/helpers"
import * as sdk from "@generated/backend/sdk.gen"

describe("bazaar page", () => {
  it("fetch API to be called ONCE on mount", async () => {
    const notebook = makeMe.aNotebook.please()
    const bazaarNotebooks = makeMe.bazaarNotebooks.notebooks(notebook).please()
    mockSdkMethod("bazaar", bazaarNotebooks)
    helper.component(BazaarPage).withRouter().render()
    expect(sdk.bazaar).toBeCalledTimes(1)
  })
})
