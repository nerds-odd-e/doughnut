import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebooksPage from "@/pages/NotebooksPage.vue"
import { beforeEach, describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY } from "@/composables/useNoteSidebarPeerSort"
import helper, { mockSdkService } from "@tests/helpers"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"

describe("Notebooks Page", () => {
  beforeEach(() => {
    localStorage.removeItem("doughnut.notebooksPage.sortOrder")
    localStorage.removeItem("doughnut.notebooksPage.layout")
    sessionStorage.removeItem(NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY)
  })

  it("fetch API to be called ONCE", async () => {
    const notebook = makeMe.aNotebook.please()

    const myNotebooksSpy = mockSdkService(NotebookController, "myNotebooks", {
      notebooks: [{ notebook }],
      catalogItems: makeMe.notebookCatalog.notebooks(notebook).please(),
      subscriptions: [],
    })
    helper.component(NotebooksPage).withRouter().render()
    expect(myNotebooksSpy).toBeCalledTimes(1)
  })

  describe("catalog overflow menu", () => {
    it("offers move to group without edit notebook settings", async () => {
      const nb = { ...makeMe.aNotebook.please(), name: "Owned Catalog" }
      mockSdkService(NotebookController, "myNotebooks", {
        notebooks: [{ notebook: nb }],
        catalogItems: makeMe.notebookCatalog.notebooks(nb).please(),
        subscriptions: [],
      })
      const wrapper = helper
        .component(NotebooksPage)
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()
      await flushPromises()
      await fireEvent.click(
        wrapper.get('[data-cy="notebook-catalog-overflow"]').element
      )
      await flushPromises()
      expect(screen.queryByTitle("Edit notebook settings")).toBeNull()
      expect(screen.getByTitle("Move to group")).toBeInTheDocument()
    })
  })
})
