import type { Notebook } from "@generated/donut-backend-api"
import {
  downloadNotebookExport,
  NOTEBOOK_EXPORT_BUTTON_LABEL,
} from "@/utils/notebookExport"
import { beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
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
  }

  it("downloads a catalog zip using the backend filename", async () => {
    const nb = makeMe.aNotebook.title("Q&A Notes").please()
    fetchMock.mockResponseOnce("zip-file-bytes", {
      headers: {
        "content-disposition": 'attachment; filename="Q&A Notes.zip"',
      },
    })
    await openCatalogOverflowFor(nb)

    const exportButtons = screen.getAllByTitle(NOTEBOOK_EXPORT_BUTTON_LABEL)
    await fireEvent.click(exportButtons[exportButtons.length - 1]!)
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      `/api/notebooks/${nb.id}/export`,
      expect.objectContaining({ credentials: "same-origin" })
    )
    expect(vi.mocked(saveAs).mock.calls.at(-1)?.[1]).toBe("Q&A Notes.zip")
  })

  it.each([
    {
      label: "notebook title when no Content-Disposition",
      title: "Owned Catalog",
      headers: undefined as Record<string, string> | undefined,
      expectedFilename: "Owned Catalog.zip",
    },
    {
      label: "notebook title when Content-Disposition is not printable ASCII",
      title: "筆記本",
      headers: {
        "content-disposition": 'attachment; filename="ç­.zip"',
      },
      expectedFilename: "筆記本.zip",
    },
  ])("falls back to $label", async ({ title, headers, expectedFilename }) => {
    const nb = makeMe.aNotebook.title(title).please()
    if (headers) {
      fetchMock.mockResponseOnce("zip-file-bytes", { headers })
    } else {
      fetchMock.mockResponseOnce("zip-file-bytes")
    }
    await downloadNotebookExport(nb.id, nb.name)

    expect(fetchMock).toHaveBeenCalledWith(
      `/api/notebooks/${nb.id}/export`,
      expect.objectContaining({ credentials: "same-origin" })
    )
    expect(vi.mocked(saveAs).mock.calls.at(-1)?.[1]).toBe(expectedFilename)
  })
})
