import NotebooksPage from "@/pages/NotebooksPage.vue"
import { describe, it } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import * as sdk from "@generated/backend/sdk.gen"

describe("Notebooks Page", () => {
  it("fetch API to be called ONCE", async () => {
    const notebook = makeMe.aNotebook.please()

    vi.spyOn(sdk, "myNotebooks").mockResolvedValue({
      data: {
        notebooks: [notebook],
        subscriptions: [],
      },
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    helper.component(NotebooksPage).withRouter().render()
    expect(sdk.myNotebooks).toBeCalledTimes(1)
  })
})
