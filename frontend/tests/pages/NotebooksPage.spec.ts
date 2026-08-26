import NotebooksPage from "@/pages/NotebooksPage.vue"
import { beforeEach, describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import {
  clearNotebooksPageStorage,
  mockMyNotebooks,
  mountNotebooksPage,
} from "./notebooksPageTestSupport"

describe("Notebooks Page", () => {
  beforeEach(() => {
    clearNotebooksPageStorage()
  })

  it("shows the Notebooks heading", async () => {
    mockMyNotebooks({ notebooks: [] })
    const wrapper = await mountNotebooksPage()
    expect(wrapper.get("h1").text()).toBe("Notebooks")
  })

  it("fetch API to be called ONCE", async () => {
    const notebook = makeMe.aNotebook.please()
    const myNotebooksSpy = mockMyNotebooks({
      notebooks: [{ notebook }],
    })
    myNotebooksSpy.mockClear()
    helper.component(NotebooksPage).withRouter().render()
    expect(myNotebooksSpy).toBeCalledTimes(1)
  })

  describe("catalog overflow menu", () => {
    it("offers move to group without edit notebook settings", async () => {
      const nb = makeMe.aNotebook.title("Owned Catalog").please()
      mockMyNotebooks({ notebooks: [{ notebook: nb }] })
      const wrapper = await mountNotebooksPage()
      await fireEvent.click(
        wrapper.get('[data-cy="notebook-catalog-overflow"]').element
      )
      await flushPromises()
      expect(screen.queryByTitle("Edit notebook settings")).toBeNull()
      expect(screen.getByTitle("Move to group")).toBeInTheDocument()
    })
  })
})
