import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import type { BookFull } from "@generated/donut-backend-api"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import {
  type BookReadingPageWrapper,
  bookId,
  getEpubMinimalBytes,
  getTopMathsPdfBytes,
  LAST_READ_POSITION_PATCH_DEBOUNCE_MS,
  loadBookReadingPageFixtures,
  mockBookReadingPageDefaults,
  mountBookReadingPage,
  notebookId,
  waitForEpubViewer,
  waitForPdfViewer,
  withFakeTimers,
} from "./bookReadingPageTestSupport"

const otherNotebookId = 8
const otherBookId = 702

type NotebookBook = { book: BookFull; bytes: ArrayBuffer }

function stubNotebookBooks(notebookBooks: NotebookBook[]) {
  mockSdkServiceWithImplementation(
    NotebookBooksController,
    "getBook",
    ({ path }) =>
      notebookBooks.find(
        ({ book }) => book.notebookId === String(path.notebook)
      )!.book
  )
  return vi.spyOn(globalThis, "fetch").mockImplementation((input) => {
    const { bytes } = notebookBooks.find(({ book }) =>
      String(input).endsWith(`/api/books/${book.id}/file`)
    )!
    return Promise.resolve(new Response(bytes.slice(0)))
  })
}

function otherNotebookPdfBook(): NotebookBook {
  return {
    book: makeMe.aBook
      .id(otherBookId)
      .notebookId(String(otherNotebookId))
      .blocks(makeMe.bookReading.topMathsLikeFlatBlocks({}))
      .please(),
    bytes: getTopMathsPdfBytes(),
  }
}

const bookBlockTitles = (wrapper: BookReadingPageWrapper) =>
  wrapper
    .findAll('[data-testid="book-reading-book-block"]')
    .map((row) => row.text())

describe("BookReadingPage moving to another notebook's book", () => {
  beforeAll(async () => {
    await loadBookReadingPageFixtures()
  })

  beforeEach(() => {
    vi.restoreAllMocks()
    mockBookReadingPageDefaults()
  })

  it("shows the other notebook's book and saves the reading position to it", async () => {
    const fetchSpy = stubNotebookBooks([
      {
        book: makeMe.aBook.id(bookId).notebookId(String(notebookId)).please(),
        bytes: getTopMathsPdfBytes(),
      },
      otherNotebookPdfBook(),
    ])
    const patchSpy = mockSdkService(
      NotebookBooksController,
      "patchNotebookBookReadingPosition",
      undefined
    )
    const wrapper = mountBookReadingPage(notebookId)
    await waitForPdfViewer(wrapper)

    await wrapper.setProps({ notebookId: otherNotebookId })
    await flushPromises()
    await waitForPdfViewer(wrapper)

    expect(bookBlockTitles(wrapper)).toContain("Section 1")
    expect(String(fetchSpy.mock.calls.at(-1)?.[0])).toMatch(
      `/api/books/${otherBookId}/file`
    )
    await withFakeTimers(async () => {
      wrapper.findComponent(PdfBookViewer).vm.$emit("viewportAnchorPage", {
        anchorPageIndexZeroBased: 1,
        viewport: { top: 100, mid: 300, bottom: 600 },
        pagesCount: 10,
      })
      vi.advanceTimersByTime(LAST_READ_POSITION_PATCH_DEBOUNCE_MS)
      await flushPromises()
    })
    expect(patchSpy).toHaveBeenCalledTimes(1)
    expect(patchSpy).toHaveBeenCalledWith(
      expect.objectContaining({ path: { notebook: otherNotebookId } })
    )
  })

  it("shows the other notebook's PDF book after an EPUB book", async () => {
    stubNotebookBooks([
      {
        book: makeMe.aBook
          .id(bookId)
          .notebookId(String(notebookId))
          .format("epub")
          .blocks([])
          .please(),
        bytes: getEpubMinimalBytes(),
      },
      otherNotebookPdfBook(),
    ])
    const wrapper = mountBookReadingPage(notebookId)
    await waitForEpubViewer(wrapper)

    await wrapper.setProps({ notebookId: otherNotebookId })
    await waitForPdfViewer(wrapper)

    expect(wrapper.find('[data-testid="epub-book-viewer"]').exists()).toBe(
      false
    )
    expect(bookBlockTitles(wrapper)).toContain("Section 1")
  })
})
