import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import { mountFirstBlockBboxScenario } from "./bookReadingPageSnapTestSupport"
import {
  clickBookBlockAndExpectSelection,
  emitViewportAndSettleCurrentBlock,
  readingControlPanel,
} from "./bookReadingPageInteractionTestSupport"
import {
  mockIsLastContentBottomVisible,
  mockReadingPanelAnchorTopPx,
} from "./bookReadingPagePdfViewerTestSupport"
import {
  loadBookReadingPageFixtures,
  mockBookReadingPageDefaults,
} from "./bookReadingPageTestSupport"

describe("BookReadingPage snap panel geometry", () => {
  beforeAll(async () => {
    await loadBookReadingPageFixtures()
  })

  beforeEach(() => {
    vi.restoreAllMocks()
    mockBookReadingPageDefaults()
  })

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
