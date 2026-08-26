import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
import { wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import {
  markSection1ReadViaPanel,
  mountSection2DirectContentBboxBook,
} from "./bookReadingPagePanelTestSupport"
import {
  bookBlockRowStartingWith,
  clickBookBlockAndExpectSelection,
  currentSelectionText,
  emitViewportAndSettleCurrentBlock,
  readingControlPanel,
} from "./bookReadingPageInteractionTestSupport"
import {
  loadBookReadingPageFixtures,
  mockBookReadingPageDefaults,
  mountLoadedBookWithBlocks,
  notebookId,
} from "./bookReadingPageTestSupport"

describe("BookReadingPage reading control panel marking", () => {
  beforeAll(async () => {
    await loadBookReadingPageFixtures()
  })

  beforeEach(() => {
    vi.restoreAllMocks()
    mockBookReadingPageDefaults()
  })

  it("marking successor via auto-targeted panel advances selection past successor", async () => {
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
    vi.spyOn(
      NotebookBooksController,
      "putNotebookBookBlockReadingRecord"
    ).mockResolvedValue(
      wrapSdkResponse([
        {
          bookBlockId: "102",
          status: "READ",
          completedAt: "2020-01-01T00:00:00Z",
        },
      ])
    )
    const wrapper = await mountSection2DirectContentBboxBook({
      mockSuccessorBottomVisible: true,
    })

    await bookBlockRowStartingWith(wrapper, "Section 1").trigger("click")
    await flushPromises()

    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 0,
      viewport: { top: 0, mid: 100, bottom: 300 },
      pagesCount: 10,
    })

    await wrapper.findComponent(ReadingControlPanel).vm.$emit("markAsRead")
    await flushPromises()

    expect(
      NotebookBooksController.putNotebookBookBlockReadingRecord
    ).toHaveBeenCalledWith(
      expect.objectContaining({
        path: expect.objectContaining({ bookBlock: 102 }),
      })
    )
    expect(currentSelectionText(wrapper)).toBe("Section 3")
  })

  it("calls PUT with SKIMMED when Skim is used", async () => {
    let putCall = 0
    const putSpy = vi
      .spyOn(NotebookBooksController, "putNotebookBookBlockReadingRecord")
      .mockImplementation(async () => {
        putCall++
        if (putCall === 1) {
          return wrapSdkResponse([])
        }
        return wrapSdkResponse([
          {
            bookBlockId: "101",
            status: "SKIMMED",
            completedAt: "2020-01-01T00:00:00Z",
          },
        ])
      })
    const wrapper = await mountLoadedBookWithBlocks(notebookId)
    await clickBookBlockAndExpectSelection(wrapper, "Section 1")

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
          bookBlock: 101,
        }),
        body: { status: "SKIMMED" },
      })
    )
  })

  it("unmounts the reading control panel after Read and advances selection to successor", async () => {
    const putSpy = vi.spyOn(
      NotebookBooksController,
      "putNotebookBookBlockReadingRecord"
    )
    const wrapper = await mountLoadedBookWithBlocks(notebookId)
    await markSection1ReadViaPanel(wrapper, putSpy)

    expect(
      bookBlockRowStartingWith(wrapper, "Section 1").attributes(
        "data-direct-content-read"
      )
    ).toBe("true")
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
            bookBlockId: "101",
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
          bookBlock: 101,
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
          bookBlockId: "101",
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
