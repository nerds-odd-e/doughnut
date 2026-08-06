import { flushPromises } from "@vue/test-utils"
import { expect, vi } from "vitest"
import {
  CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS,
  withFakeTimers,
  type BookReadingPageWrapper,
} from "./bookReadingPageTestSupport"
import { findPdfBookViewer } from "./bookReadingPagePdfViewerTestSupport"

export async function emitViewportAndSettleCurrentBlock(
  wrapper: BookReadingPageWrapper,
  payload: {
    anchorPageIndexZeroBased: number
    viewport: { top: number; mid: number; bottom: number } | null
    pagesCount: number
  }
) {
  const pdf = findPdfBookViewer(wrapper)
  await withFakeTimers(async () => {
    pdf.vm.$emit("viewportAnchorPage", payload)
    await vi.advanceTimersByTimeAsync(CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS)
    await flushPromises()
  })
}

export function readingControlPanel(wrapper: BookReadingPageWrapper) {
  return wrapper.find('[data-testid="book-reading-reading-control-panel"]')
}

export function currentSelectionText(wrapper: BookReadingPageWrapper): string {
  return wrapper.find('[data-current-selection="true"]').text()
}

export function expectCurrentSelection(
  wrapper: BookReadingPageWrapper,
  title: string
) {
  expect(currentSelectionText(wrapper)).toBe(title)
}

export function bookBlockRowStartingWith(
  wrapper: BookReadingPageWrapper,
  titlePrefix: string
) {
  const row = wrapper
    .findAll('[data-testid="book-reading-book-block"]')
    .find((w) => w.text().trim().startsWith(titlePrefix))
  expect(row, `book block row "${titlePrefix}"`).toBeDefined()
  return row!
}

export async function clickBookBlockByTitle(
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

export async function clickBookBlockAndExpectSelection(
  wrapper: BookReadingPageWrapper,
  title: string
) {
  await clickBookBlockByTitle(wrapper, title)
  expect(currentSelectionText(wrapper)).toBe(title)
}

export async function clickBookBlockStartingWithAndExpectSelection(
  wrapper: BookReadingPageWrapper,
  titlePrefix: string
) {
  await bookBlockRowStartingWith(wrapper, titlePrefix).trigger("click")
  await flushPromises()
  expect(currentSelectionText(wrapper)).toMatch(new RegExp(`^${titlePrefix}`))
}
