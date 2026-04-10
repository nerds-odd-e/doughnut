import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import { AUTO_SELECT_BOOK_BLOCK_DWELL_MS } from "@/composables/useBookReadingBlockSelection"
import BookReadingPage from "@/pages/BookReadingPage.vue"
import type { BookBlockFull } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import helper, { wrapSdkResponse } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import createFetchMock from "vitest-fetch-mock"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import topMathsUrl from "../../../e2e_test/fixtures/book_reading/top-maths.pdf?url"

const fetchMock = createFetchMock(vi)

/** Keep in sync with `BookReadingPage.vue` */
const CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS = 120
const LAST_READ_POSITION_PATCH_DEBOUNCE_MS = 400

const notebookId = 7

let topMathsPdfBytes!: ArrayBuffer

function bookFileUrlSuffix(id: number) {
  return `/api/notebooks/${id}/book/file`
}

function topMathsLikeFlatBlocks(options?: {
  firstBlockHasNoDirectContent?: boolean
}): BookBlockFull[] {
  const anchors = makeMe.bookReadingTopMathsLikeAnchors()
  return anchors.map((startAnchor, i) => ({
    id: i + 1,
    title: `Section ${i + 1}`,
    startAnchor,
    siblingOrder: i,
    hasDirectContent: !(options?.firstBlockHasNoDirectContent && i === 0),
  }))
}

function fetchRequestUrl(input: RequestInfo | URL): string {
  return typeof input === "string"
    ? input
    : input instanceof URL
      ? input.href
      : input.url
}

function mockNotebookBookFilePdfOk(
  id: number,
  pdfBytes: ArrayBuffer,
  options?: { assertSameOriginCredentials?: boolean }
) {
  const suffix = bookFileUrlSuffix(id)
  vi.spyOn(globalThis, "fetch").mockImplementation((input, init) => {
    const url = fetchRequestUrl(input)
    if (!url.endsWith(suffix)) {
      return Promise.reject(new Error(`unexpected fetch: ${url}`))
    }
    if (options?.assertSameOriginCredentials) {
      expect(init?.credentials).toBe("same-origin")
    }
    return Promise.resolve(
      new Response(pdfBytes.slice(0), {
        status: 200,
        headers: { "Content-Type": "application/pdf" },
      })
    )
  })
}

function mountBookReadingPage(id: number) {
  return helper
    .component(BookReadingPage)
    .withRouter()
    .withProps({ notebookId: id })
    .mount()
}

type BookReadingPageWrapper = ReturnType<typeof mountBookReadingPage>

async function waitForPdfViewer(wrapper: BookReadingPageWrapper) {
  await vi.waitFor(
    () =>
      expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(
        true
      ),
    { timeout: 8000 }
  )
}

async function withStubbedInnerWidth<T>(
  width: number,
  run: () => Promise<T>
): Promise<T> {
  const innerWidthDesc = Object.getOwnPropertyDescriptor(window, "innerWidth")
  Object.defineProperty(window, "innerWidth", {
    configurable: true,
    writable: true,
    value: width,
  })
  try {
    return await run()
  } finally {
    if (innerWidthDesc) {
      Object.defineProperty(window, "innerWidth", innerWidthDesc)
    }
  }
}

async function withFakeTimers(run: () => Promise<void>) {
  vi.useFakeTimers()
  try {
    await run()
  } finally {
    vi.useRealTimers()
  }
}

function stubGetBookPlain() {
  return vi
    .spyOn(NotebookBooksController, "getBook")
    .mockResolvedValue(wrapSdkResponse(makeMe.aBook.please()))
}

function stubGetBookWithTopMathsBlocks(firstBlockHasNoDirectContent?: boolean) {
  return vi
    .spyOn(NotebookBooksController, "getBook")
    .mockResolvedValue(
      wrapSdkResponse(
        makeMe.aBook
          .blocks(topMathsLikeFlatBlocks({ firstBlockHasNoDirectContent }))
          .please()
      )
    )
}

async function mountLoadedBookWithBlocks(
  id: number,
  options?: {
    innerWidth?: number
    assertSameOriginCredentials?: boolean
    firstBlockHasNoDirectContent?: boolean
  }
) {
  stubGetBookWithTopMathsBlocks(options?.firstBlockHasNoDirectContent)
  mockNotebookBookFilePdfOk(id, topMathsPdfBytes, {
    assertSameOriginCredentials: options?.assertSameOriginCredentials,
  })
  const mount = async () => {
    const wrapper = mountBookReadingPage(id)
    await waitForPdfViewer(wrapper)
    return wrapper
  }
  if (options?.innerWidth !== undefined) {
    return withStubbedInnerWidth(options.innerWidth, mount)
  }
  return mount()
}

function pdfScrollRestoreSpy(wrapper: BookReadingPageWrapper) {
  const pdf = wrapper.findComponent(PdfBookViewer)
  const exposed = (
    pdf.vm as unknown as {
      $: {
        exposed: {
          scrollToStoredReadingPosition: (
            page: number,
            y: number
          ) => Promise<void>
        }
      }
    }
  ).$.exposed
  return { pdf, spy: vi.spyOn(exposed, "scrollToStoredReadingPosition") }
}

async function mountPatchDebounceScenario() {
  stubGetBookWithTopMathsBlocks(undefined)
  mockNotebookBookFilePdfOk(notebookId, topMathsPdfBytes)
  const patchSpy = vi
    .spyOn(NotebookBooksController, "patchNotebookBookReadingPosition")
    .mockResolvedValue(wrapSdkResponse(undefined))
  const wrapper = mountBookReadingPage(notebookId)
  await waitForPdfViewer(wrapper)
  return { wrapper, patchSpy }
}

describe("BookReadingPage", () => {
  beforeAll(async () => {
    fetchMock.disableMocks()
    const res = await fetch(topMathsUrl)
    topMathsPdfBytes = await res.arrayBuffer()
    fetchMock.enableMocks()
    fetchMock.doMock()
  })

  beforeEach(() => {
    vi.restoreAllMocks()
    vi.spyOn(
      NotebookBooksController,
      "getNotebookBookReadingPosition"
    ).mockResolvedValue(
      wrapSdkResponse(undefined) as Awaited<
        ReturnType<
          typeof NotebookBooksController.getNotebookBookReadingPosition
        >
      >
    )
    vi.spyOn(
      NotebookBooksController,
      "getNotebookBookReadingRecords"
    ).mockResolvedValue(wrapSdkResponse([]))
  })

  it("shows fetch error when book file returns an error status", async () => {
    stubGetBookPlain()
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(null, { status: 404 })
    )

    const wrapper = mountBookReadingPage(notebookId)
    await flushPromises()

    const err = wrapper.find('[data-testid="book-reading-pdf-error"]')
    expect(err.exists()).toBe(true)
    expect(err.text()).toBe("Could not load the book file.")
    expect(wrapper.find(".daisy-loading-spinner").exists()).toBe(false)
    expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(false)
  })

  it("does not load PDF viewer when hasSourceFile is false", async () => {
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(makeMe.aBook.hasSourceFile(false).please())
    )

    const wrapper = mountBookReadingPage(notebookId)
    await flushPromises()

    expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(false)
    expect(
      wrapper.find('[data-testid="book-reading-pdf-error"]').exists()
    ).toBe(false)
  })

  it("shows loading indicator while PDF is loading, hides it after render", async () => {
    stubGetBookPlain()
    let resolveFetch!: (r: Response) => void
    vi.spyOn(globalThis, "fetch").mockReturnValue(
      new Promise<Response>((resolve) => {
        resolveFetch = resolve
      })
    )

    const wrapper = mountBookReadingPage(notebookId)
    await flushPromises()

    expect(wrapper.find(".daisy-loading-spinner").exists()).toBe(true)
    expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(false)

    resolveFetch(
      new Response(topMathsPdfBytes.slice(0), {
        status: 200,
        headers: { "Content-Type": "application/pdf" },
      })
    )

    await flushPromises()
    await vi.waitFor(
      () => expect(wrapper.find(".daisy-loading-spinner").exists()).toBe(false),
      { timeout: 5000 }
    )
  })

  it("loads PDF into viewer when hasSourceFile is true", async () => {
    stubGetBookPlain()
    mockNotebookBookFilePdfOk(notebookId, topMathsPdfBytes, {
      assertSameOriginCredentials: true,
    })

    const wrapper = mountBookReadingPage(notebookId)
    await waitForPdfViewer(wrapper)
  })

  it("shows error when PDF bytes are not valid", async () => {
    stubGetBookPlain()
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(new TextEncoder().encode("not a pdf").buffer, {
        status: 200,
        headers: { "Content-Type": "text/plain" },
      })
    )

    const wrapper = mountBookReadingPage(notebookId)

    await vi.waitFor(
      () => {
        expect(
          wrapper.find('[data-testid="book-reading-pdf-error"]').exists()
        ).toBe(true)
      },
      { timeout: 5000 }
    )

    expect(wrapper.find('[data-testid="book-reading-pdf-error"]').text()).toBe(
      "This file is not a valid PDF."
    )
    expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(false)
  })

  it("updates current block while book layout drawer is closed (Phase 6.9)", async () => {
    const wrapper = await mountLoadedBookWithBlocks(notebookId, {
      innerWidth: 500,
    })
    const pdf = wrapper.findComponent(PdfBookViewer)

    await withFakeTimers(async () => {
      pdf.vm.$emit("viewportAnchorPage", {
        anchorPageIndexZeroBased: 0,
        viewport: null,
        pagesCount: 10,
      })
      await vi.advanceTimersByTimeAsync(CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS)
      await flushPromises()
    })

    const indicator = wrapper.find(
      '[data-testid="book-reading-page-indicator"]'
    )
    expect(indicator.exists()).toBe(true)
    expect(indicator.text().trim()).toBe("1 / 10")

    const current = wrapper.find('[data-current-block="true"]')
    expect(current.exists()).toBe(true)
    expect(current.attributes("aria-current")).toBe("location")
    expect(current.text()).toBe("Section 3")
  })

  it("zoom buttons exist with accessible names and page indicator shows via PdfControl (Phase 12)", async () => {
    const wrapper = await mountLoadedBookWithBlocks(notebookId)
    expect(
      wrapper.find('[data-testid="pdf-zoom-in"]').attributes("aria-label")
    ).toBe("Zoom in")
    expect(
      wrapper.find('[data-testid="pdf-zoom-out"]').attributes("aria-label")
    ).toBe("Zoom out")

    const pdf = wrapper.findComponent(PdfBookViewer)
    await withFakeTimers(async () => {
      pdf.vm.$emit("viewportAnchorPage", {
        anchorPageIndexZeroBased: 0,
        viewport: null,
        pagesCount: 5,
      })
      await vi.advanceTimersByTimeAsync(CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS)
      await flushPromises()
    })

    const indicator = wrapper.find(
      '[data-testid="book-reading-page-indicator"]'
    )
    expect(indicator.exists()).toBe(true)
    expect(indicator.text().trim()).toBe("1 / 5")
  })

  it("debounces PATCH reading position on rapid viewport updates (Phase 1.3)", async () => {
    const { wrapper, patchSpy } = await mountPatchDebounceScenario()
    const pdf = wrapper.findComponent(PdfBookViewer)
    const viewport = { top: 200, mid: 500, bottom: 1000 }

    await withFakeTimers(async () => {
      for (let i = 0; i < 3; i++) {
        pdf.vm.$emit("viewportAnchorPage", {
          anchorPageIndexZeroBased: 2,
          viewport,
          pagesCount: 10,
        })
      }
      expect(patchSpy).not.toHaveBeenCalled()
      await vi.advanceTimersByTimeAsync(LAST_READ_POSITION_PATCH_DEBOUNCE_MS)
      await flushPromises()
    })

    expect(patchSpy).toHaveBeenCalledTimes(1)
    expect(patchSpy).toHaveBeenCalledWith({
      path: { notebook: notebookId },
      body: { pageIndex: 2, normalizedY: 200 },
    })
  })

  it("PATCH reading position uses last viewport top within debounce window (Phase 1.3)", async () => {
    const { wrapper, patchSpy } = await mountPatchDebounceScenario()
    const pdf = wrapper.findComponent(PdfBookViewer)

    await withFakeTimers(async () => {
      pdf.vm.$emit("viewportAnchorPage", {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 50, mid: 100, bottom: 200 },
        pagesCount: 10,
      })
      pdf.vm.$emit("viewportAnchorPage", {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 150, mid: 250, bottom: 300 },
        pagesCount: 10,
      })
      await vi.advanceTimersByTimeAsync(LAST_READ_POSITION_PATCH_DEBOUNCE_MS)
      await flushPromises()
    })

    expect(patchSpy).toHaveBeenCalledTimes(1)
    expect(patchSpy.mock.calls[0]?.[0]).toEqual({
      path: { notebook: notebookId },
      body: { pageIndex: 0, normalizedY: 150 },
    })
  })

  it("does not PATCH reading position when viewport is null (Phase 1.3)", async () => {
    const { wrapper, patchSpy } = await mountPatchDebounceScenario()
    const pdf = wrapper.findComponent(PdfBookViewer)

    await withFakeTimers(async () => {
      pdf.vm.$emit("viewportAnchorPage", {
        anchorPageIndexZeroBased: 0,
        viewport: null,
        pagesCount: 10,
      })
      pdf.vm.$emit("viewportAnchorPage", {
        anchorPageIndexZeroBased: 1,
        viewport: null,
        pagesCount: 10,
      })
      await vi.advanceTimersByTimeAsync(LAST_READ_POSITION_PATCH_DEBOUNCE_MS)
      await flushPromises()
    })

    expect(patchSpy).not.toHaveBeenCalled()
  })

  it("restores reading position from stored snapshot on open (Phase 1.4)", async () => {
    stubGetBookPlain()
    vi.spyOn(
      NotebookBooksController,
      "getNotebookBookReadingPosition"
    ).mockResolvedValue(
      wrapSdkResponse({ id: 1, pageIndex: 2, normalizedY: 750 })
    )
    mockNotebookBookFilePdfOk(notebookId, topMathsPdfBytes)

    const wrapper = mountBookReadingPage(notebookId)
    await waitForPdfViewer(wrapper)

    const { pdf, spy } = pdfScrollRestoreSpy(wrapper)
    pdf.vm.$emit("pagesReady")
    await flushPromises()

    expect(spy).toHaveBeenCalledWith(2, 750)
  })

  it("does not restore reading position when no snapshot exists (Phase 1.4)", async () => {
    stubGetBookPlain()
    mockNotebookBookFilePdfOk(notebookId, topMathsPdfBytes)

    const wrapper = mountBookReadingPage(notebookId)
    await waitForPdfViewer(wrapper)

    const { pdf, spy } = pdfScrollRestoreSpy(wrapper)
    pdf.vm.$emit("pagesReady")
    await flushPromises()

    expect(spy).not.toHaveBeenCalled()
  })

  it("book layout toggle exposes aria-expanded and aria-controls (Phase 7.7)", async () => {
    await withStubbedInnerWidth(1024, async () => {
      const wrapper = await mountLoadedBookWithBlocks(notebookId)

      const toggle = wrapper.find(
        '[data-testid="book-reading-book-layout-toggle"]'
      )
      const aside = wrapper.find(
        '[data-testid="book-reading-book-layout-aside"]'
      )
      expect(aside.attributes("id")).toBe("book-reading-book-layout-panel")
      expect(toggle.attributes("aria-controls")).toBe(
        "book-reading-book-layout-panel"
      )
      expect(toggle.attributes("aria-expanded")).toBe("true")

      await toggle.trigger("click")
      expect(toggle.attributes("aria-expanded")).toBe("false")

      await toggle.trigger("click")
      expect(toggle.attributes("aria-expanded")).toBe("true")
    })
  })

  describe("Reading control panel", () => {
    function readingControlPanel(wrapper: BookReadingPageWrapper) {
      return wrapper.find('[data-testid="book-reading-reading-control-panel"]')
    }

    async function clickBookBlockByTitle(
      wrapper: BookReadingPageWrapper,
      title: string
    ) {
      const row = wrapper
        .findAll('[data-testid="book-reading-book-block"]')
        .find((w) => w.text() === title)
      expect(row, `book block row "${title}"`).toBeDefined()
      await row!.trigger("click")
      await flushPromises()
    }

    async function emitViewportAndSettleCurrentBlock(
      wrapper: BookReadingPageWrapper,
      payload: {
        anchorPageIndexZeroBased: number
        viewport: { top: number; mid: number; bottom: number } | null
        pagesCount: number
      }
    ) {
      const pdf = wrapper.findComponent(PdfBookViewer)
      await withFakeTimers(async () => {
        pdf.vm.$emit("viewportAnchorPage", payload)
        await vi.advanceTimersByTimeAsync(CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS)
        await flushPromises()
      })
    }

    it("shows read border for blocks returned as READ from reading-records on load", async () => {
      vi.spyOn(
        NotebookBooksController,
        "getNotebookBookReadingRecords"
      ).mockResolvedValue(
        wrapSdkResponse([
          {
            bookBlockId: "1",
            status: "READ",
            completedAt: "2020-01-01T00:00:00Z",
          },
        ])
      )
      const wrapper = await mountLoadedBookWithBlocks(notebookId)
      const section1Row = wrapper
        .findAll('[data-testid="book-reading-book-block"]')
        .find((w) => w.text().trim().startsWith("Section 1"))
      expect(section1Row?.attributes("data-direct-content-read")).toBe("true")
    })

    it("shows the panel when the selected block’s successor is the viewport current block", async () => {
      const wrapper = await mountLoadedBookWithBlocks(notebookId)
      await clickBookBlockByTitle(wrapper, "Section 1")
      await vi.waitFor(() =>
        expect(wrapper.find('[data-current-selection="true"]').text()).toBe(
          "Section 1"
        )
      )

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 500, bottom: 1000 },
        pagesCount: 10,
      })

      const current = wrapper.find('[data-current-block="true"]')
      expect(current.text()).toBe("Section 2")

      const panel = readingControlPanel(wrapper)
      expect(panel.exists()).toBe(true)
      expect(panel.isVisible()).toBe(true)
      expect(panel.text()).toContain("Section 1")
      expect(
        wrapper.find('[data-testid="book-reading-mark-as-read"]').exists()
      ).toBe(true)
      expect(
        wrapper.find('[data-testid="book-reading-mark-as-skimmed"]').exists()
      ).toBe(true)
      expect(
        wrapper.find('[data-testid="book-reading-mark-as-skipped"]').exists()
      ).toBe(true)
    })

    it("shows skimmed border for blocks returned as SKIMMED from reading-records on load", async () => {
      vi.spyOn(
        NotebookBooksController,
        "getNotebookBookReadingRecords"
      ).mockResolvedValue(
        wrapSdkResponse([
          {
            bookBlockId: "1",
            status: "SKIMMED",
            completedAt: "2020-01-01T00:00:00Z",
          },
        ])
      )
      const wrapper = await mountLoadedBookWithBlocks(notebookId)
      const section1Row = wrapper
        .findAll('[data-testid="book-reading-book-block"]')
        .find((w) => w.text().trim().startsWith("Section 1"))
      expect(section1Row?.attributes("data-direct-content-skimmed")).toBe(
        "true"
      )
      expect(
        section1Row?.attributes("data-direct-content-read")
      ).toBeUndefined()
    })

    it("hides the panel when nothing is selected", async () => {
      const wrapper = await mountLoadedBookWithBlocks(notebookId)

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 500, bottom: 1000 },
        pagesCount: 10,
      })

      expect(readingControlPanel(wrapper).exists()).toBe(false)
    })

    it("hides the panel when the current block is not the immediate successor of the selection", async () => {
      const wrapper = await mountLoadedBookWithBlocks(notebookId)
      await clickBookBlockByTitle(wrapper, "Section 1")
      await vi.waitFor(() =>
        expect(wrapper.find('[data-current-selection="true"]').text()).toBe(
          "Section 1"
        )
      )

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 400, mid: 600, bottom: 1000 },
        pagesCount: 10,
      })

      expect(wrapper.find('[data-current-block="true"]').text()).toBe(
        "Section 3"
      )
      expect(readingControlPanel(wrapper).exists()).toBe(false)
    })

    it("hides the panel when the selected block has no successor", async () => {
      const wrapper = await mountLoadedBookWithBlocks(notebookId)
      await clickBookBlockByTitle(wrapper, "Section 6")
      await vi.waitFor(() =>
        expect(wrapper.find('[data-current-selection="true"]').text()).toBe(
          "Section 6"
        )
      )

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 1,
        viewport: null,
        pagesCount: 10,
      })

      expect(wrapper.find('[data-current-block="true"]').text()).toBe(
        "Section 6"
      )
      expect(readingControlPanel(wrapper).exists()).toBe(false)
    })

    it("calls PUT with SKIMMED when Mark as skimmed is used", async () => {
      const putSpy = vi
        .spyOn(NotebookBooksController, "putNotebookBookBlockReadingRecord")
        .mockResolvedValue(
          wrapSdkResponse([
            {
              bookBlockId: "1",
              status: "SKIMMED",
              completedAt: "2020-01-01T00:00:00Z",
            },
          ])
        )
      const wrapper = await mountLoadedBookWithBlocks(notebookId)
      await clickBookBlockByTitle(wrapper, "Section 1")
      await vi.waitFor(() =>
        expect(wrapper.find('[data-current-selection="true"]').text()).toBe(
          "Section 1"
        )
      )

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 500, bottom: 1000 },
        pagesCount: 10,
      })

      await wrapper.findComponent(ReadingControlPanel).vm.$emit("markAsSkimmed")
      await flushPromises()

      expect(putSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: expect.objectContaining({
            notebook: notebookId,
            bookBlock: 1,
          }),
          body: { status: "SKIMMED" },
        })
      )
    })

    it("moves book layout selection to the successor block after Mark as read", async () => {
      vi.spyOn(
        NotebookBooksController,
        "putNotebookBookBlockReadingRecord"
      ).mockResolvedValue(
        wrapSdkResponse([
          {
            bookBlockId: "1",
            status: "READ",
            completedAt: "2020-01-01T00:00:00Z",
          },
        ])
      )
      const wrapper = await mountLoadedBookWithBlocks(notebookId)
      await clickBookBlockByTitle(wrapper, "Section 1")
      await vi.waitFor(() =>
        expect(wrapper.find('[data-current-selection="true"]').text()).toBe(
          "Section 1"
        )
      )

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 500, bottom: 1000 },
        pagesCount: 10,
      })

      await wrapper.findComponent(ReadingControlPanel).vm.$emit("markAsRead")
      await flushPromises()

      expect(wrapper.find('[data-current-selection="true"]').text()).toBe(
        "Section 2"
      )
      expect(wrapper.find('[data-current-block="true"]').text()).toBe(
        "Section 2"
      )
    })

    it("unmounts the reading control panel after Mark as read once it was shown", async () => {
      vi.spyOn(
        NotebookBooksController,
        "putNotebookBookBlockReadingRecord"
      ).mockResolvedValue(
        wrapSdkResponse([
          {
            bookBlockId: "1",
            status: "READ",
            completedAt: "2020-01-01T00:00:00Z",
          },
        ])
      )
      const wrapper = await mountLoadedBookWithBlocks(notebookId)
      await clickBookBlockByTitle(wrapper, "Section 1")
      await vi.waitFor(() =>
        expect(wrapper.find('[data-current-selection="true"]').text()).toBe(
          "Section 1"
        )
      )

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 500, bottom: 1000 },
        pagesCount: 10,
      })

      const panel = wrapper.findComponent(ReadingControlPanel)
      expect(panel.exists()).toBe(true)

      const selectedRow = wrapper.find('[data-current-selection="true"]')
      expect(selectedRow.attributes("data-direct-content-read")).toBeUndefined()

      await panel.vm.$emit("markAsRead")
      await flushPromises()

      const section1Row = wrapper
        .findAll('[data-testid="book-reading-book-block"]')
        .find((w) => /^Section 1(?:\s|$)/.test(w.text().trim()))
      expect(section1Row?.attributes("data-direct-content-read")).toBe("true")
      expect(wrapper.find('[data-current-selection="true"]').text()).toBe(
        "Section 2"
      )
      expect(readingControlPanel(wrapper).exists()).toBe(false)
    })

    it("auto-marks predecessor with READ body when it has no direct content and no record", async () => {
      const putSpy = vi
        .spyOn(NotebookBooksController, "putNotebookBookBlockReadingRecord")
        .mockResolvedValue(
          wrapSdkResponse([
            {
              bookBlockId: "1",
              status: "READ",
              completedAt: "2020-01-01T00:00:00Z",
            },
          ])
        )
      const wrapper = await mountLoadedBookWithBlocks(notebookId, {
        firstBlockHasNoDirectContent: true,
      })

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 500, bottom: 1000 },
        pagesCount: 10,
      })
      await flushPromises()

      expect(putSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: expect.objectContaining({
            notebook: notebookId,
            bookBlock: 1,
          }),
          body: { status: "READ" },
        })
      )
    })

    it("does not auto-mark when predecessor has no direct content but is already SKIMMED", async () => {
      vi.spyOn(
        NotebookBooksController,
        "getNotebookBookReadingRecords"
      ).mockResolvedValue(
        wrapSdkResponse([
          {
            bookBlockId: "1",
            status: "SKIMMED",
            completedAt: "2020-01-01T00:00:00Z",
          },
        ])
      )
      const putSpy = vi
        .spyOn(NotebookBooksController, "putNotebookBookBlockReadingRecord")
        .mockResolvedValue(wrapSdkResponse([]))

      const wrapper = await mountLoadedBookWithBlocks(notebookId, {
        firstBlockHasNoDirectContent: true,
      })

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 500, bottom: 1000 },
        pagesCount: 10,
      })
      await flushPromises()

      expect(putSpy).not.toHaveBeenCalled()
    })
  })

  describe("Book block dwell selection", () => {
    it("sets selection to the viewport current block after dwell", async () => {
      const wrapper = await mountLoadedBookWithBlocks(notebookId)
      vi.useFakeTimers()
      try {
        const pdf = wrapper.findComponent(PdfBookViewer)
        pdf.vm.$emit("viewportAnchorPage", {
          anchorPageIndexZeroBased: 0,
          viewport: { top: 0, mid: 500, bottom: 1000 },
          pagesCount: 10,
        })
        await vi.advanceTimersByTimeAsync(CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS)
        await flushPromises()
        expect(wrapper.find('[data-current-selection="true"]').exists()).toBe(
          false
        )

        await vi.advanceTimersByTimeAsync(AUTO_SELECT_BOOK_BLOCK_DWELL_MS)
        await flushPromises()

        const current = wrapper.find('[data-current-block="true"]')
        const selected = wrapper.find('[data-current-selection="true"]')
        expect(selected.exists()).toBe(true)
        expect(selected.text()).toBe(current.text())
      } finally {
        vi.useRealTimers()
      }
    })

    it("resets dwell when the current block changes before dwell elapses", async () => {
      const wrapper = await mountLoadedBookWithBlocks(notebookId)
      vi.useFakeTimers()
      try {
        const pdf = wrapper.findComponent(PdfBookViewer)
        pdf.vm.$emit("viewportAnchorPage", {
          anchorPageIndexZeroBased: 0,
          viewport: { top: 0, mid: 500, bottom: 1000 },
          pagesCount: 10,
        })
        await vi.advanceTimersByTimeAsync(CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS)
        await flushPromises()
        expect(wrapper.find('[data-current-block="true"]').text()).toBe(
          "Section 2"
        )

        await vi.advanceTimersByTimeAsync(AUTO_SELECT_BOOK_BLOCK_DWELL_MS - 150)
        pdf.vm.$emit("viewportAnchorPage", {
          anchorPageIndexZeroBased: 0,
          viewport: { top: 400, mid: 600, bottom: 1000 },
          pagesCount: 10,
        })
        await vi.advanceTimersByTimeAsync(CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS)
        await flushPromises()
        expect(wrapper.find('[data-current-block="true"]').text()).toBe(
          "Section 3"
        )
        expect(wrapper.find('[data-current-selection="true"]').exists()).toBe(
          false
        )

        await vi.advanceTimersByTimeAsync(200)
        await flushPromises()
        expect(wrapper.find('[data-current-selection="true"]').exists()).toBe(
          false
        )

        await vi.advanceTimersByTimeAsync(AUTO_SELECT_BOOK_BLOCK_DWELL_MS)
        await flushPromises()
        expect(wrapper.find('[data-current-selection="true"]').text()).toBe(
          "Section 3"
        )
      } finally {
        vi.useRealTimers()
      }
    })
  })
})
