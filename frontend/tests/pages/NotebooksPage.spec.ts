import NotebooksPage from "@/pages/NotebooksPage.vue"
import { describe, it } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"

describe("Notebooks Page", () => {
  it("fetch API to be called ONCE", async () => {
    const notebook = makeMe.aNotebook.please()

    const myNotebooksSpy = mockSdkService("myNotebooks", {
      notebooks: [notebook],
      subscriptions: [],
    })
    helper.component(NotebooksPage).withRouter().render()
    expect(myNotebooksSpy).toBeCalledTimes(1)
  })
})
