import { BOOK_READING_LAYOUT_BREAKPOINT_PX } from "@/lib/book-reading/bookReadingLayoutBreakpoint"
import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import {
  setupGlobalClient,
  teardownGlobalClientForTesting,
} from "@/managedApi/clientSetup"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { wrapSdkResponse } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  bookReadingContentProps,
  clickAiReorganize,
  loadingModal,
  mockToast,
  mountBookReadingContent,
  mountBookReadingWithGlobalModal,
} from "./bookReadingContentAiReorganizeTestSupport"

vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))

type SuggestResult = Awaited<
  ReturnType<typeof NotebookBooksController.suggestBookLayoutReorganization>
>
type ApplyResult = Awaited<
  ReturnType<typeof NotebookBooksController.applyBookLayoutReorganization>
>

function mockPendingSuggest() {
  let resolveSuggest: (value: SuggestResult) => void = () => undefined
  vi.mocked(
    NotebookBooksController.suggestBookLayoutReorganization
  ).mockImplementation(
    () =>
      new Promise((resolve) => {
        resolveSuggest = resolve
      }) as ReturnType<
        typeof NotebookBooksController.suggestBookLayoutReorganization
      >
  )
  return {
    resolve: (value: SuggestResult) => resolveSuggest(value),
  }
}

function mockPendingApply() {
  let resolveApply: (value: ApplyResult) => void = () => undefined
  vi.spyOn(
    NotebookBooksController,
    "applyBookLayoutReorganization"
  ).mockImplementation(
    () =>
      new Promise((resolve) => {
        resolveApply = resolve
      }) as ReturnType<
        typeof NotebookBooksController.applyBookLayoutReorganization
      >
  )
  return {
    resolve: (value: ApplyResult) => resolveApply(value),
  }
}

describe("BookReadingContent AI reorganize suggest", () => {
  const apiStatus: ApiStatus = { states: [] }
  let innerWidthDesc: PropertyDescriptor | undefined

  beforeEach(() => {
    apiStatus.states = []
    mockToast.error.mockClear()
    setupGlobalClient(apiStatus)

    innerWidthDesc = Object.getOwnPropertyDescriptor(window, "innerWidth")
    Object.defineProperty(window, "innerWidth", {
      configurable: true,
      writable: true,
      value: BOOK_READING_LAYOUT_BREAKPOINT_PX + 100,
    })

    vi.spyOn(
      NotebookBooksController,
      "getNotebookBookReadingRecords"
    ).mockResolvedValue(wrapSdkResponse([]))

    vi.spyOn(
      NotebookBooksController,
      "suggestBookLayoutReorganization"
    ).mockResolvedValue({
      data: undefined,
      error: { message: "suggest failed" },
      request: {} as Request,
      response: { status: 500 } as Response,
    })
  })

  afterEach(() => {
    if (innerWidthDesc) {
      Object.defineProperty(window, "innerWidth", innerWidthDesc)
    }
    teardownGlobalClientForTesting()
  })

  it("shows error toast when suggest fails", async () => {
    const wrapper = mountBookReadingContent(bookReadingContentProps())

    await flushPromises()
    await clickAiReorganize(wrapper)
    await flushPromises()

    expect(mockToast.error).toHaveBeenCalled()
    wrapper.unmount()
  })

  it("opens preview dialog with canonical title when suggest succeeds, Cancel closes", async () => {
    vi.mocked(
      NotebookBooksController.suggestBookLayoutReorganization
    ).mockResolvedValueOnce(wrapSdkResponse({ blocks: [{ id: 1, depth: 0 }] }))

    const wrapper = mountBookReadingContent(bookReadingContentProps())

    await flushPromises()
    await clickAiReorganize(wrapper)
    await flushPromises()

    const dialog = wrapper.find(
      '[data-testid="book-layout-reorganize-preview-dialog"]'
    )
    expect(dialog.classes()).toContain("daisy-modal-open")
    expect(wrapper.text()).toContain("Reorganize layout (preview)")

    await wrapper
      .find('[data-testid="book-layout-reorganize-preview-cancel"]')
      .trigger("click")
    await flushPromises()

    expect(dialog.classes()).not.toContain("daisy-modal-open")
    wrapper.unmount()
  })

  it("preview lists blocks with suggested depths and highlights changed rows", async () => {
    const book = makeMe.aBook
      .notebookId("9")
      .blocks([
        makeMe.aBookBlock.id(1).depth(0).title("Alpha").please(),
        makeMe.aBookBlock.id(2).depth(0).title("Beta").please(),
      ])
      .please()

    vi.mocked(
      NotebookBooksController.suggestBookLayoutReorganization
    ).mockResolvedValueOnce(
      wrapSdkResponse({
        blocks: [
          { id: 1, depth: 0 },
          { id: 2, depth: 1 },
        ],
      })
    )

    const wrapper = mountBookReadingContent(bookReadingContentProps(book))

    await flushPromises()
    await clickAiReorganize(wrapper)
    await flushPromises()

    const rows = wrapper.findAll(
      '[data-testid="book-layout-reorganize-preview-row"]'
    )
    expect(rows).toHaveLength(2)
    expect(rows[0]!.attributes("data-suggested-depth")).toBe("0")
    expect(rows[0]!.classes()).not.toContain("bg-warning/15")
    expect(rows[1]!.attributes("data-suggested-depth")).toBe("1")
    expect(rows[1]!.classes()).toContain("bg-warning/15")

    wrapper.unmount()
  })

  it("shows the global loading modal while suggest API is pending", async () => {
    const pending = mockPendingSuggest()
    const wrapper = mountBookReadingWithGlobalModal(bookReadingContentProps())

    await flushPromises()
    await clickAiReorganize(wrapper)
    await flushPromises()

    expect(loadingModal()).toBeTruthy()
    expect(document.body.textContent).toContain("Analyzing book layout…")

    pending.resolve(wrapSdkResponse({ blocks: [{ id: 1, depth: 0 }] }))
    await flushPromises()

    expect(loadingModal()).toBeNull()
    wrapper.unmount()
  })

  it("shows the global loading modal while apply API is pending", async () => {
    const book = makeMe.aBook
      .notebookId("9")
      .blocks([makeMe.aBookBlock.id(1).depth(0).title("A").please()])
      .please()

    vi.mocked(
      NotebookBooksController.suggestBookLayoutReorganization
    ).mockResolvedValueOnce(wrapSdkResponse({ blocks: [{ id: 1, depth: 1 }] }))

    const pending = mockPendingApply()
    const wrapper = mountBookReadingWithGlobalModal(
      bookReadingContentProps(book)
    )

    await flushPromises()
    await clickAiReorganize(wrapper)
    await flushPromises()

    await wrapper
      .find('[data-testid="book-layout-reorganize-preview-confirm"]')
      .trigger("click")
    await flushPromises()

    expect(loadingModal()).toBeTruthy()
    expect(document.body.textContent).toContain("Applying layout changes…")

    pending.resolve(
      wrapSdkResponse({
        ...book,
        blocks: [{ id: 1, depth: 1, title: "A", contentLocators: [] }],
      })
    )
    await flushPromises()

    expect(loadingModal()).toBeNull()
    wrapper.unmount()
  })
})
