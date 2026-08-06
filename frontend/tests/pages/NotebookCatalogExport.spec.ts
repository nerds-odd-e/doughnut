import type { Notebook } from "@generated/doughnut-backend-api"
import { NOTEBOOK_EXPORT_BUTTON_LABEL } from "@/utils/notebookExport"
import { beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { saveAs } from "file-saver"
import createFetchMock from "vitest-fetch-mock"
import {
  clearNotebooksPageStorage,
  mockMyNotebooks,
  mountNotebooksPage,
} from "./notebooksPageTestSupport"

vi.mock("file-saver", () => ({ saveAs: vi.fn() }))

const fetchMock = createFetchMock(vi)
fetchMock.enableMocks()

describe("Notebook catalog export", () => {
  beforeEach(() => {
    clearNotebooksPageStorage()
    fetchMock.resetMocks()
    vi.mocked(saveAs).mockClear()
  })

  async function openCatalogOverflowFor(notebook: Notebook) {
    mockMyNotebooks({ notebooks: [{ notebook }] })
    const wrapper = await mountNotebooksPage()
    await fireEvent.click(
      wrapper.get('[data-cy="notebook-catalog-overflow"]').element
    )
    await flushPromises()
    return wrapper
  }

  it("downloads a zip when export is clicked", async () => {
    const nb = makeMe.aNotebook.title("Owned Catalog").please()
    fetchMock.mockResponseOnce("zip-file-bytes")
    await openCatalogOverflowFor(nb)

    const exportButtons = screen.getAllByTitle(NOTEBOOK_EXPORT_BUTTON_LABEL)
    await fireEvent.click(exportButtons[exportButtons.length - 1]!)
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      `/api/notebooks/${nb.id}/export`,
      expect.objectContaining({ credentials: "same-origin" })
    )
    expect(vi.mocked(saveAs).mock.calls[0]![1]).toBe("Owned Catalog.zip")
  })

  it("uses the sanitized filename the backend sends via Content-Disposition", async () => {
    const nb = makeMe.aNotebook.title("Q&A: Notes").please()
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
    const nb = makeMe.aNotebook.title("筆記本").please()
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
