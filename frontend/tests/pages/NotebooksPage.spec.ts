import NotebooksPage from "@/pages/NotebooksPage.vue"
import { describe, it } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import * as sdk from "@generated/backend/sdk.gen"

describe("Notebooks Page", () => {
  it("fetch API to be called ONCE", async () => {
    const notebook = makeMe.aNotebook.please()

    mockSdkService("myNotebooks", {
      notebooks: [notebook],
      subscriptions: [],
    })
    helper.component(NotebooksPage).withRouter().render()
    expect(sdk.myNotebooks).toBeCalledTimes(1)
  })
})
