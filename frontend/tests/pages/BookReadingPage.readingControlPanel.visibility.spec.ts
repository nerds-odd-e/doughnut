import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
import { wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import { mountSection2DirectContentBboxBook } from "./bookReadingPagePanelTestSupport"
import {
  bookBlockRowStartingWith,
  clickBookBlockAndExpectSelection,
  emitViewportAndSettleCurrentBlock,
  expectCurrentSelection,
  readingControlPanel,
} from "./bookReadingPageInteractionTestSupport"
import { mockIsLastContentBottomVisible } from "./bookReadingPagePdfViewerTestSupport"
import {
  loadBookReadingPageFixtures,
  mockBookReadingPageDefaults,
  mountLoadedBookWithBlocks,
  notebookId,
} from "./bookReadingPageTestSupport"

describe("BookReadingPage reading control panel visibility", () => {
  beforeAll(async () => {
    await loadBookReadingPageFixtures()
  })

  beforeEach(() => {
    vi.restoreAllMocks()
    mockBookReadingPageDefaults()
  })

  it.each([
    {
      status: "READ" as const,
      expectRead: "true",
      expectSkimmed: undefined,
    },
    {
      status: "SKIMMED" as const,
      expectRead: undefined,
      expectSkimmed: "true",
    },
  ])(
    "shows $status border for blocks returned as $status from reading-records on load",
    async ({ status, expectRead, expectSkimmed }) => {
      vi.spyOn(
        NotebookBooksController,
        "getNotebookBookReadingRecords"
      ).mockResolvedValue(
        wrapSdkResponse([
          {
            bookBlockId: "101",
            status,
            completedAt: "2020-01-01T00:00:00Z",
          },
        ])
      )
      const wrapper = await mountLoadedBookWithBlocks(notebookId)
      const section1Row = bookBlockRowStartingWith(wrapper, "Section 1")
      expect(section1Row.attributes("data-direct-content-read")).toBe(
        expectRead
      )
      expect(section1Row.attributes("data-direct-content-skimmed")).toBe(
        expectSkimmed
      )
    }
  )

  it("shows the panel when the selected block’s successor is the viewport current block", async () => {
    const wrapper = await mountLoadedBookWithBlocks(notebookId)
    await clickBookBlockAndExpectSelection(wrapper, "Section 1")

    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 0,
      viewport: { top: 0, mid: 500, bottom: 1000 },
      pagesCount: 10,
    })

    const panel = readingControlPanel(wrapper)
    expect(panel.exists()).toBe(true)
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

  it("hides the Reading Control Panel when the default-selected first block is viewport current", async () => {
    const wrapper = await mountLoadedBookWithBlocks(notebookId)
    expectCurrentSelection(wrapper, "Section 1")

    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 0,
      viewport: { top: 0, mid: 10, bottom: 1000 },
      pagesCount: 10,
    })

    expect(readingControlPanel(wrapper).exists()).toBe(false)
  })

  it("hides the panel when the current block is not the immediate successor of the selection", async () => {
    const wrapper = await mountLoadedBookWithBlocks(notebookId)
    await clickBookBlockAndExpectSelection(wrapper, "Section 1")

    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 0,
      viewport: { top: 400, mid: 600, bottom: 1000 },
      pagesCount: 10,
    })

    expect(readingControlPanel(wrapper).exists()).toBe(false)
  })

  it("hides the panel when the last block has direct content but bottom is not visible", async () => {
    const wrapper = await mountLoadedBookWithBlocks(notebookId, {
      lastBlockHasDirectContent: true,
    })
    mockIsLastContentBottomVisible(wrapper, false)
    await clickBookBlockAndExpectSelection(wrapper, "Section 6")

    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 1,
      viewport: null,
      pagesCount: 10,
    })

    expect(readingControlPanel(wrapper).exists()).toBe(false)
  })

  it("shows the panel for the last block when content bottom is visible", async () => {
    const wrapper = await mountLoadedBookWithBlocks(notebookId, {
      lastBlockHasDirectContent: true,
    })
    mockIsLastContentBottomVisible(wrapper, true)
    await clickBookBlockAndExpectSelection(wrapper, "Section 6")

    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 1,
      viewport: null,
      pagesCount: 10,
    })

    const panel = readingControlPanel(wrapper)
    expect(panel.exists()).toBe(true)
    expect(panel.text()).toContain("Section 6")
  })

  it("shows panel for successor when selected block is already marked and successor bottom is visible", async () => {
    vi.spyOn(
      NotebookBooksController,
      "getNotebookBookReadingRecords"
    ).mockResolvedValue(
      wrapSdkResponse([
        {
          bookBlockId: "101",
          status: "READ",
          completedAt: "2020-01-01T00:00:00Z",
        },
      ])
    )
    const wrapper = await mountSection2DirectContentBboxBook({
      mockSuccessorBottomVisible: true,
    })

    const section1Row = bookBlockRowStartingWith(wrapper, "Section 1")
    await section1Row.trigger("click")
    await flushPromises()

    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 0,
      viewport: { top: 0, mid: 100, bottom: 300 },
      pagesCount: 10,
    })

    expect(readingControlPanel(wrapper).text()).toContain("Section 2")
  })
})
