import {
  ensureOnBookReadingPage,
  epubElementSpansFromReaderTopPx,
  type EpubReaderElementQuery,
  type EpubReaderElementSpan,
} from './bookReadingShared'

/** The reader's scrolled view: epub.js's stage inside the viewer. */
export const EPUB_READER_VIEW =
  '[data-testid="epub-book-viewer"] .epub-container'

const HEADINGS = 'h1,h2,h3,h4,h5,h6'

/** How far (px) an element's edge may sit from the reader top and still count as "at the top". */
export const TOP_TOLERANCE_PX = 8

/** How the reader steps find an element: a heading by its text, a paragraph by its opening text. */
type EpubReaderElement = EpubReaderElementQuery & { label: string }

export const heading = (text: string): EpubReaderElement => ({
  label: `heading "${text}"`,
  selector: HEADINGS,
  text,
  textMatch: 'exact',
})

export const paragraph = (text: string): EpubReaderElement => ({
  label: `paragraph "${text}"`,
  selector: 'p',
  text,
  textMatch: 'start',
})

/** The element is rendered exactly once and its span passes `check`. */
export function expectEpubElementSpan(
  el: EpubReaderElement,
  check: (span: EpubReaderElementSpan) => void
) {
  ensureOnBookReadingPage()
  cy.get(EPUB_READER_VIEW, { timeout: 30000 }).should(($c) => {
    const spans = epubElementSpansFromReaderTopPx($c.get(0) as HTMLElement, el)
    expect(spans, `EPUB ${el.label} rendered`).to.have.length(1)
    const [span] = spans
    if (span)
      check({ top: Math.round(span.top), bottom: Math.round(span.bottom) })
  })
}

/**
 * Scrolls the reader down in steps until the element is rendered, then scrolls its top
 * `passPx` above the top of the reader's view.
 */
export function scrollEpubReaderUntilElementPassesTop(
  el: EpubReaderElement,
  passPx: number
) {
  ensureOnBookReadingPage()
  const maxSteps = 48
  const step = (n: number): Cypress.Chainable =>
    cy.get(EPUB_READER_VIEW, { timeout: 30000 }).then(($c) => {
      const container = $c.get(0) as HTMLElement
      const [span] = epubElementSpansFromReaderTopPx(container, el)
      if (span !== undefined) {
        // epub.js drops the first scroll event after its own silent scroll adjustment, so a
        // single jump may go unreported; a reader's scroll is many events, and a final 1 px
        // step stands in for them.
        container.scrollTop += span.top + passPx + 1
        cy.wait(300)
        return cy.then(() => {
          container.scrollTop -= 1
          cy.wait(300)
        })
      }
      if (n >= maxSteps) {
        throw new Error(
          `EPUB ${el.label} not rendered after ${maxSteps} scroll steps`
        )
      }
      container.scrollTop += Math.ceil(container.clientHeight * 0.85)
      cy.wait(200)
      return step(n + 1)
    })
  return cy.then(() => step(0))
}
