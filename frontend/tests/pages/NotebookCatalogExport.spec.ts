import type { Notebook } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebooksPage from "@/pages/NotebooksPage.vue"
import { NOTEBOOK_EXPORT_BUTTON_LABEL } from "@/utils/notebookExport"
import { beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY } from "@/composables/useNoteSidebarPeerSort"
import helper, { mockSdkService } from "@tests/helpers"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { saveAs } from "file-saver"
import createFetchMock from "vitest-fetch-mock"

vi.mock("file-saver", () => ({ saveAs: vi.fn() }))

const fetchMock = createFetchMock(vi)
fetchMock.enableMocks()

describe("Notebook catalog export", () => {
  beforeEach(() => {
    localStorage.removeItem("doughnut.notebooksPage.sortOrder")
    localStorage.removeItem("doughnut.notebooksPage.layout")
    sessionStorage.removeItem(NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY)
    fetchMock.resetMocks()
    vi.mocked(saveAs).mockClear()
  })

  async function openCatalogOverflowFor(notebook: Notebook) {
    mockSdkService(NotebookController, "myNotebooks", {
      notebooks: [{ notebook }],
      catalogItems: makeMe.notebookCatalog.notebooks(notebook).please(),
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
    return wrapper
  }

  it("downloads a zip when export is clicked", async () => {
    const nb = { ...makeMe.aNotebook.please(), name: "Owned Catalog" }
    fetchMock.mockResponseOnce("zip-file-bytes")
    await openCatalogOverflowFor(nb)

    // Teleported dropdown portals from earlier mounts are never unmounted
    // (no explicit wrapper.unmount()), so more than one portal can be present;
    // the freshest one (this test's own) is always last in DOM order.
    const exportButtons = screen.getAllByTitle(NOTEBOOK_EXPORT_BUTTON_LABEL)
    await fireEvent.click(exportButtons[exportButtons.length - 1]!)
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      `/api/notebooks/${nb.id}/export`,
      expect.objectContaining({ credentials: "same-origin" })
    )
    expect(saveAs).toHaveBeenCalled()
    expect(vi.mocked(saveAs).mock.calls[0][1]).toBe("Owned Catalog.zip")
  })

  it("uses the sanitized filename the backend sends via Content-Disposition", async () => {
    const nb = { ...makeMe.aNotebook.please(), name: "Q&A: Notes" }
    fetchMock.mockResponseOnce("zip-file-bytes", {
      headers: {
        "content-disposition": 'attachment; filename="Q&A Notes.zip"',
      },
    })
    await openCatalogOverflowFor(nb)

    const exportButtons = screen.getAllByTitle(NOTEBOOK_EXPORT_BUTTON_LABEL)
    await fireEvent.click(exportButtons[exportButtons.length - 1]!)
    await flushPromises()

    expect(vi.mocked(saveAs).mock.calls.at(-1)?.[1]).toBe("Q&A Notes.zip")
  })

  it("falls back to the notebook name when Content-Disposition is not printable ASCII", async () => {
    const nb = { ...makeMe.aNotebook.please(), name: "筆記本" }
    // Representative of a Content-Disposition header Spring's ISO-8859-1
    // header encoding mangled from a non-ASCII notebook name.
    fetchMock.mockResponseOnce("zip-file-bytes", {
      headers: {
        "content-disposition": 'attachment; filename="ç­.zip"',
      },
    })
    await openCatalogOverflowFor(nb)

    const exportButtons = screen.getAllByTitle(NOTEBOOK_EXPORT_BUTTON_LABEL)
    await fireEvent.click(exportButtons[exportButtons.length - 1]!)
    await flushPromises()

    expect(vi.mocked(saveAs).mock.calls.at(-1)?.[1]).toBe("筆記本.zip")
  })
})
