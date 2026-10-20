import NotebooksPage from "@/pages/NotebooksPage.vue"
import { beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { wrapSdkError } from "@tests/helpers"
import { NotebookController } from "@generated/donut-backend-api/sdk.gen"
import {
  donutMenuLaunchersIn,
  donutMenuPanelIn,
  openDonutMenuIn,
} from "@tests/toolbars/donutMenuTestSupport"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import {
  clearNotebooksPageStorage,
  mockMyNotebooks,
  mockMyNotebooksLoading,
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

  it("keeps the Donut menu while the catalog loads", async () => {
    vi.spyOn(NotebookController, "myNotebooks").mockReturnValue(
      new Promise(() => undefined) as never
    )
    const wrapper = await mountNotebooksPage()
    expect(donutMenuLaunchersIn(wrapper)).toHaveLength(1)
  })

  it("keeps the Donut menu open when the catalog finishes loading", async () => {
    const finishLoading = mockMyNotebooksLoading({ notebooks: [] })
    const wrapper = await mountNotebooksPage()
    await openDonutMenuIn(wrapper)
    expect(donutMenuPanelIn(wrapper).exists()).toBe(true)

    finishLoading()
    await flushPromises()

    expect(wrapper.get("h1").text()).toBe("Notebooks")
    expect(donutMenuPanelIn(wrapper).exists()).toBe(true)
    expect(donutMenuLaunchersIn(wrapper)).toHaveLength(1)
  })

  it("keeps the Donut menu when the catalog cannot be fetched", async () => {
    vi.spyOn(NotebookController, "myNotebooks").mockResolvedValue(
      wrapSdkError("server error")
    )
    const wrapper = await mountNotebooksPage()
    expect(donutMenuLaunchersIn(wrapper)).toHaveLength(1)
  })

  it("keeps one Donut menu beside the loaded Notebooks heading", async () => {
    mockMyNotebooks({ notebooks: [] })
    const wrapper = await mountNotebooksPage()
    expect(donutMenuLaunchersIn(wrapper)).toHaveLength(1)
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
