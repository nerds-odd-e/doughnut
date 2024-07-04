import NotebooksPage from "@/pages/NotebooksPage.vue"
import { describe, it } from "vitest"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"

describe("Notebooks Page", () => {
  beforeEach(() => {
    const teleportTarget = document.createElement("div")
    teleportTarget.id = "head-status"
    document.body.appendChild(teleportTarget)
  })

  it("fetch API to be called ONCE", async () => {
    const notebook = makeMe.aNotebook.please()

    helper.managedApi.restNotebookController.myNotebooks = vi
      .fn()
      .mockResolvedValue({
        notebooks: [notebook],
        subscriptions: [],
      })
    helper.component(NotebooksPage).withProps({}).render()
    expect(
      helper.managedApi.restNotebookController.myNotebooks
    ).toBeCalledTimes(1)
  })
})
