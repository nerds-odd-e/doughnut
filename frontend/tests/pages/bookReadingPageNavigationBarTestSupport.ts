import {
  clickBookBlockAndExpectSelection,
  emitViewportAndSettleCurrentBlock,
} from "./bookReadingPageInteractionTestSupport"
import { spyOnScrollToBookNavTarget } from "./bookReadingPagePdfViewerTestSupport"
import {
  mountLoadedBookWithBlocks,
  notebookId,
  type BookReadingPageWrapper,
} from "./bookReadingPageTestSupport"

export async function mountNavBarScenario(viewportMid: number) {
  const wrapper = await mountLoadedBookWithBlocks(notebookId)
  spyOnScrollToBookNavTarget(wrapper)
  await clickBookBlockAndExpectSelection(wrapper, "Section 1")
  await emitViewportAndSettleCurrentBlock(wrapper, {
    anchorPageIndexZeroBased: 0,
    viewport: { top: 0, mid: viewportMid, bottom: 1000 },
    pagesCount: 10,
  })
  return wrapper
}

export function currentBlockNavBar(wrapper: BookReadingPageWrapper) {
  return wrapper.find('[data-testid="current-block-navigation-bar"]')
}
