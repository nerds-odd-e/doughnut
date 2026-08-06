import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import {
  emitSuccessorCrossing,
  expireSnapHold,
  mountFirstBlockBboxScenario,
  mountIndependentSnapBudgetsScenario,
  selectSection1WithVisibleGeometry,
} from "./bookReadingPageSnapTestSupport"
import {
  bookBlockRowStartingWith,
  clickBookBlockAndExpectSelection,
  currentSelectionText,
  emitViewportAndSettleCurrentBlock,
} from "./bookReadingPageInteractionTestSupport"
import { mockIsLastContentBottomVisible } from "./bookReadingPagePdfViewerTestSupport"
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

describe("BookReadingPage snap budgets", () => {
  beforeAll(async () => {
    await loadBookReadingPageFixtures()
  })

  beforeEach(() => {
    vi.restoreAllMocks()
    mockBookReadingPageDefaults()
    snapHoldActivateMock.fn.mockClear()
  })

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
