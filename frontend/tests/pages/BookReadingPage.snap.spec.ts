import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
import { wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import {
  emitSuccessorCrossing,
  expireSnapHold,
  mountCrossPageBboxScenario,
  mountFirstBlockBboxScenario,
  mountIndependentSnapBudgetsScenario,
  mountNoDirectContentBboxScenario,
  selectSection1WithVisibleGeometry,
} from "./bookReadingPageSnapTestSupport"
import {
  bookBlockRowStartingWith,
  clickBookBlockAndExpectSelection,
  clickBookBlockStartingWithAndExpectSelection,
  currentSelectionText,
  emitViewportAndSettleCurrentBlock,
  readingControlPanel,
} from "./bookReadingPageInteractionTestSupport"
import {
  mockIsLastContentBottomVisible,
  mockReadingPanelAnchorTopPx,
  spyOnScrollPageNormalizedYToReadingClearance,
} from "./bookReadingPagePdfViewerTestSupport"
import {
  loadBookReadingPageFixtures,
  mockBookReadingPageDefaults,
} from "./bookReadingPageTestSupport"

const snapHoldActivateMock = vi.hoisted(() => ({
  fn: vi.fn<(ms: number) => void>(),
}))

vi.mock(
  "@/lib/book-reading/intervalScrollSuppression",
  async (importOriginal) => {
    const actual =
      await importOriginal<
        typeof import("@/lib/book-reading/intervalScrollSuppression")
      >()
    return {
      ...actual,
      createIntervalScrollSuppression: () => {
        const real = actual.createIntervalScrollSuppression()
        return {
          ...real,
          activate: (ms: number) => {
            snapHoldActivateMock.fn(ms)
            real.activate(ms)
          },
        }
      },
    }
  }
)

describe("BookReadingPage snap", () => {
  beforeAll(async () => {
    await loadBookReadingPageFixtures()
  })

  beforeEach(() => {
    vi.restoreAllMocks()
    mockBookReadingPageDefaults()
    snapHoldActivateMock.fn.mockClear()
  })

  it("snaps back on first boundary crossing and when landing two+ blocks ahead", async () => {
    const wrapper = await mountFirstBlockBboxScenario({
      contentFitsInViewport: true,
    })
    const snapToBottomSpy =
      spyOnScrollPageNormalizedYToReadingClearance(wrapper)

    await selectSection1WithVisibleGeometry(wrapper)
    expect(readingControlPanel(wrapper).exists()).toBe(true)

    await emitSuccessorCrossing(wrapper)

    expect(snapHoldActivateMock.fn).toHaveBeenCalledWith(500)
    expect(snapToBottomSpy).not.toHaveBeenCalled()
    expect(readingControlPanel(wrapper).exists()).toBe(true)

    snapHoldActivateMock.fn.mockClear()
    await expireSnapHold()
    await clickBookBlockAndExpectSelection(wrapper, "Section 1")
    await selectSection1WithVisibleGeometry(wrapper)
    await emitSuccessorCrossing(wrapper, { mid: 600, bottom: 1000 })

    expect(snapHoldActivateMock.fn).toHaveBeenCalledWith(500)
    expect(readingControlPanel(wrapper).exists()).toBe(true)
  })

  it("snaps back on second crossing, allows scrolling on third, and stays unsnapped on fourth", async () => {
    const wrapper = await mountFirstBlockBboxScenario({
      contentFitsInViewport: true,
    })

    await selectSection1WithVisibleGeometry(wrapper)
    await emitSuccessorCrossing(wrapper)
    expect(snapHoldActivateMock.fn).toHaveBeenCalledTimes(1)
    expect(readingControlPanel(wrapper).exists()).toBe(true)

    await expireSnapHold()
    await emitSuccessorCrossing(wrapper)
    expect(snapHoldActivateMock.fn).toHaveBeenCalledTimes(2)
    expect(readingControlPanel(wrapper).exists()).toBe(true)

    await expireSnapHold()
    await emitSuccessorCrossing(wrapper)
    expect(snapHoldActivateMock.fn).toHaveBeenCalledTimes(2)
    expect(readingControlPanel(wrapper).exists()).toBe(false)

    await emitSuccessorCrossing(wrapper)
    expect(snapHoldActivateMock.fn).toHaveBeenCalledTimes(2)
  })

  it("does not snap when block has no recorded direct-content bbox", async () => {
    const wrapper = await mountNoDirectContentBboxScenario()

    await clickBookBlockAndExpectSelection(wrapper, "Section 1")

    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 0,
      viewport: { top: 0, mid: 500, bottom: 1000 },
      pagesCount: 10,
    })

    expect(wrapper.find('[data-current-block="true"]').text()).toBe("Section 2")
    expect(readingControlPanel(wrapper).exists()).toBe(true)
  })

  it("does not snap when geometry was never visible for the selection", async () => {
    const wrapper = await mountFirstBlockBboxScenario({
      lastContentBottomVisible: false,
    })

    await clickBookBlockAndExpectSelection(wrapper, "Section 1")

    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 0,
      viewport: { top: 0, mid: 200, bottom: 600 },
      pagesCount: 10,
    })

    expect(snapHoldActivateMock.fn).not.toHaveBeenCalled()
    expect(wrapper.find('[data-current-block="true"]').text()).toBe("Section 2")
  })

  it("does not snap when block already has a recorded disposition", async () => {
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
    const wrapper = await mountFirstBlockBboxScenario()
    await clickBookBlockStartingWithAndExpectSelection(wrapper, "Section 1")
    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 0,
      viewport: { top: 0, mid: 40, bottom: 600 },
      pagesCount: 10,
    })
    await emitSuccessorCrossing(wrapper)

    expect(snapHoldActivateMock.fn).not.toHaveBeenCalled()
    expect(wrapper.find('[data-current-block="true"]').text()).toBe("Section 2")
  })

  it("snap state resets when selection changes to a different block", async () => {
    const wrapper = await mountFirstBlockBboxScenario({
      contentFitsInViewport: true,
    })

    await selectSection1WithVisibleGeometry(wrapper)
    await emitSuccessorCrossing(wrapper)
    expect(snapHoldActivateMock.fn).toHaveBeenCalledTimes(1)

    await clickBookBlockAndExpectSelection(wrapper, "Section 2")
    expect(snapHoldActivateMock.fn).toHaveBeenCalledTimes(1)
  })

  it("snaps to last bbox bottom when start anchor and last content bbox are on different pages", async () => {
    const wrapper = await mountCrossPageBboxScenario()
    const snapToBottomSpy =
      spyOnScrollPageNormalizedYToReadingClearance(wrapper)

    await selectSection1WithVisibleGeometry(wrapper)
    await emitSuccessorCrossing(wrapper)

    expect(snapToBottomSpy).toHaveBeenCalledWith(1, 150, 80)
    expect(readingControlPanel(wrapper).exists()).toBe(true)
  })

  it("same-page-too-tall: snaps to last content bottom when content does not fit with panel", async () => {
    const wrapper = await mountFirstBlockBboxScenario({
      contentFitsInViewport: false,
    })
    const snapToBottomSpy =
      spyOnScrollPageNormalizedYToReadingClearance(wrapper)

    await selectSection1WithVisibleGeometry(wrapper)
    await emitSuccessorCrossing(wrapper)

    expect(snapToBottomSpy).toHaveBeenCalledWith(0, 750, 80)
    expect(readingControlPanel(wrapper).exists()).toBe(true)
  })

  it("sets and clears data-snap-animating when snap fires and animation ends", async () => {
    const wrapper = await mountFirstBlockBboxScenario({
      contentFitsInViewport: true,
    })
    await selectSection1WithVisibleGeometry(wrapper)
    await emitSuccessorCrossing(wrapper)

    expect(readingControlPanel(wrapper).attributes("data-snap-animating")).toBe(
      "true"
    )

    const panel = readingControlPanel(wrapper)
    const card = panel.element.querySelector("div")
    card?.dispatchEvent(new Event("animationend"))
    await wrapper.vm.$nextTick()

    expect(
      readingControlPanel(wrapper).attributes("data-snap-animating")
    ).toBeUndefined()
  })

  describe("budgets", () => {
    it("marking READ clears snap reminder: block no longer snaps when re-visited", async () => {
      vi.spyOn(
        NotebookBooksController,
        "putNotebookBookBlockReadingRecord"
      ).mockResolvedValue(
        wrapSdkResponse([
          {
            bookBlockId: "101",
            status: "READ",
            completedAt: "2020-01-01T00:00:00Z",
          },
        ])
      )
      const wrapper = await mountFirstBlockBboxScenario({
        contentFitsInViewport: true,
      })

      await selectSection1WithVisibleGeometry(wrapper)
      await emitSuccessorCrossing(wrapper)
      expect(snapHoldActivateMock.fn).toHaveBeenCalledTimes(1)

      await wrapper.findComponent(ReadingControlPanel).vm.$emit("markAsRead")
      await flushPromises()
      expect(currentSelectionText(wrapper)).toBe("Section 2")

      await bookBlockRowStartingWith(wrapper, "Section 1").trigger("click")
      await flushPromises()

      mockIsLastContentBottomVisible(wrapper, true)
      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 40, bottom: 600 },
        pagesCount: 10,
      })
      await emitSuccessorCrossing(wrapper)

      expect(snapHoldActivateMock.fn).toHaveBeenCalledTimes(1)
    })

    it("different unread blocks get independent snap budgets", async () => {
      const wrapper = await mountIndependentSnapBudgetsScenario()

      await clickBookBlockAndExpectSelection(wrapper, "Section 1")

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 40, bottom: 600 },
        pagesCount: 10,
      })
      mockIsLastContentBottomVisible(wrapper, false)

      await emitSuccessorCrossing(wrapper)
      await expireSnapHold()

      await emitSuccessorCrossing(wrapper)
      await expireSnapHold()

      expect(snapHoldActivateMock.fn).toHaveBeenCalledTimes(2)

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 200, bottom: 600 },
        pagesCount: 10,
      })
      expect(snapHoldActivateMock.fn).toHaveBeenCalledTimes(2)

      await clickBookBlockAndExpectSelection(wrapper, "Section 2")

      mockIsLastContentBottomVisible(wrapper, true)
      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 140, bottom: 600 },
        pagesCount: 10,
      })

      mockIsLastContentBottomVisible(wrapper, false)
      await emitSuccessorCrossing(wrapper, {
        top: 201,
        mid: 640,
        bottom: 1000,
      })
      await expireSnapHold()

      await emitSuccessorCrossing(wrapper, {
        top: 201,
        mid: 640,
        bottom: 1000,
      })

      expect(snapHoldActivateMock.fn).toHaveBeenCalledTimes(4)
    })
  })

  describe("panel geometry", () => {
    it("shows the panel when last content bottom is visible and above obstruction", async () => {
      const wrapper = await mountFirstBlockBboxScenario()
      await clickBookBlockAndExpectSelection(wrapper, "Section 1")

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 200, bottom: 600 },
        pagesCount: 10,
      })

      expect(readingControlPanel(wrapper).exists()).toBe(true)
    })

    it("anchors panel when last content bottom is visible and anchor px is returned", async () => {
      const wrapper = await mountFirstBlockBboxScenario()
      mockReadingPanelAnchorTopPx(wrapper, 120)
      await clickBookBlockAndExpectSelection(wrapper, "Section 1")

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 200, bottom: 600 },
        pagesCount: 10,
      })

      const panel = readingControlPanel(wrapper)
      expect(panel.exists()).toBe(true)
      expect(panel.attributes("data-panel-placement")).toBe("anchored")
      expect((panel.element as HTMLElement).style.top).toBe("120px")
      expect((panel.element as HTMLElement).style.bottom).toBe("auto")
    })

    it("hides the panel when last content bottom is not yet above obstruction", async () => {
      const wrapper = await mountFirstBlockBboxScenario({
        lastContentBottomVisible: false,
      })
      await clickBookBlockAndExpectSelection(wrapper, "Section 1")

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 200, bottom: 600 },
        pagesCount: 10,
      })

      expect(readingControlPanel(wrapper).exists()).toBe(false)
    })

    it("keeps panel visible after geometry becomes false while successor is not yet current", async () => {
      const wrapper = await mountFirstBlockBboxScenario()
      await clickBookBlockAndExpectSelection(wrapper, "Section 1")

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 40, bottom: 600 },
        pagesCount: 10,
      })
      expect(readingControlPanel(wrapper).exists()).toBe(true)

      mockIsLastContentBottomVisible(wrapper, false)
      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 0, mid: 40, bottom: 600 },
        pagesCount: 10,
      })

      expect(readingControlPanel(wrapper).exists()).toBe(true)
      expect(
        readingControlPanel(wrapper).attributes("data-panel-placement")
      ).toBe("fixed")
    })
  })
})
