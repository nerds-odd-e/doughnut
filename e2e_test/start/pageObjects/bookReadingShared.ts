import { waitUntilAppIsNotBusy } from '../pageBase'

export type BookLayoutRow = { depth: number; title: string }

/** Same geometry rule as `expectEpubContentTextVisible` (marker must intersect `.epub-container`). */
export function epubHostViewportIntersectsMarker(
  host: HTMLElement,
  text: string
): boolean {
  const hostRect = host.getBoundingClientRect()
  const iframe = [...host.querySelectorAll('iframe')].find((f) =>
    (f.contentDocument?.body?.innerText ?? '').includes(text)
  ) as HTMLIFrameElement | undefined
  if (!iframe?.contentDocument?.body) {
    return false
  }
  const doc = iframe.contentDocument
  let best: HTMLElement | undefined
  let bestLen = Number.POSITIVE_INFINITY
  for (const node of doc.body.querySelectorAll('*')) {
    const e = node as HTMLElement
    const t = e.textContent ?? ''
    if (!t.includes(text) || t.length > bestLen) continue
    bestLen = t.length
    best = e
  }
  if (!best) {
    return false
  }
  const iframeRect = iframe.getBoundingClientRect()
  const local = best.getBoundingClientRect()
  const absTop = iframeRect.top + local.top
  const absLeft = iframeRect.left + local.left
  const absBottom = absTop + local.height
  const absRight = absLeft + local.width
  const hOverlap =
    Math.min(absBottom, hostRect.bottom) - Math.max(absTop, hostRect.top)
  const wOverlap =
    Math.min(absRight, hostRect.right) - Math.max(absLeft, hostRect.left)
  // Allow thin table cells (e.g. "Cell One" in a single column).
  return hOverlap > 0 && wOverlap > 0
}

export const bookBlockRows = () =>
  cy
    .get('[data-testid="book-reading-book-layout"]')
    .find('[data-testid="book-reading-book-block"]')

/** Row is the block button; `.contains(text)` alone would match the inner title span. */
export const bookBlockRowByTitle = (title: string) =>
  cy
    .get('[data-testid="book-reading-book-layout"]')
    .contains('[data-testid="book-reading-book-block"]', title)

export const assertPdfCanvasIsRendered = (el: HTMLCanvasElement) => {
  expect(el.width, 'PDF canvas should have width').to.be.greaterThan(0)
  expect(el.height, 'PDF canvas should have height').to.be.greaterThan(0)
}

export const BOOK_READING_PATHNAME = /^\/notebooks\/(\d+)\/book$/

export function notebookIdFromBookReadingPathname(pathname: string): string {
  const match = pathname.match(BOOK_READING_PATHNAME)
  expect(
    match,
    `could not parse notebookId from book reading pathname: ${pathname}`
  ).to.not.be.null
  return match![1]
}

export const ensureOnBookReadingPage = () => {
  waitUntilAppIsNotBusy()
  cy.location('pathname').should('match', BOOK_READING_PATHNAME)
  cy.get('[data-testid="book-reading-page"]').should('exist')
}
