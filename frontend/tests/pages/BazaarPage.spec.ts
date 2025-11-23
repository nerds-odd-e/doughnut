import BazaarPage from "@/pages/BazaarPage.vue"
import { describe, it, expect, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import * as sdk from "@generated/backend/sdk.gen"

describe("bazaar page", () => {
  it("fetch API to be called ONCE on mount", async () => {
    const notebook = makeMe.aNotebook.please()
    const bazaarNotebooks = makeMe.bazaarNotebooks.notebooks(notebook).please()
    vi.spyOn(sdk, "bazaar").mockResolvedValue({
      data: bazaarNotebooks,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    helper.component(BazaarPage).withRouter().render()
    expect(sdk.bazaar).toBeCalledTimes(1)
  })
})
