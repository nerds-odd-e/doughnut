import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { wrapSdkResponse } from "@tests/helpers"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import {
  emitSuccessorCrossing,
  expireSnapHold,
  mountCrossPageBboxScenario,
  mountFirstBlockBboxScenario,
  mountNoDirectContentBboxScenario,
  selectSection1WithVisibleGeometry,
} from "./bookReadingPageSnapTestSupport"
import {
  clickBookBlockAndExpectSelection,
  clickBookBlockStartingWithAndExpectSelection,
  emitViewportAndSettleCurrentBlock,
  readingControlPanel,
} from "./bookReadingPageInteractionTestSupport"
import { spyOnScrollPageNormalizedYToReadingClearance } from "./bookReadingPagePdfViewerTestSupport"
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

  it("snaps back and keeps panel visible on first boundary crossing (same-page: scrolls to block start)", async () => {
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
  })

  it("snaps back when scrolling lands two or more blocks ahead (not just immediate successor)", async () => {
    const wrapper = await mountFirstBlockBboxScenario({
      contentFitsInViewport: true,
    })

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
})
