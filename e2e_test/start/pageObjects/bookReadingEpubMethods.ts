import { e2eAppBaseUrl } from '../../support/e2eAppUrl'
import { waitUntilAppIsNotBusy } from '../pageBase'
import router from '../router'
import {
  BOOK_READING_PATHNAME,
  bookBlockRowByTitle,
  ensureOnBookReadingPage,
  epubElementSpansFromReaderTopPx,
  type EpubReaderElementQuery,
  type EpubReaderElementSpan,
  epubHostViewportIntersectsMarker,
  expectUsesScreenWidth,
  epubReaderElementsWithText,
  notebookIdFromBookReadingPathname,
} from './bookReadingShared'

/** The reader's scrolled view: epub.js's stage inside the viewer. */
const EPUB_READER_VIEW = '[data-testid="epub-book-viewer"] .epub-container'

const HEADINGS = 'h1,h2,h3,h4,h5,h6'

/** How far (px) an element's edge may sit from the reader top and still count as "at the top". */
const TOP_TOLERANCE_PX = 8

/** How the reader steps find an element: a heading by its text, a paragraph by its opening text. */
type EpubReaderElement = EpubReaderElementQuery & { label: string }

const heading = (text: string): EpubReaderElement => ({
  label: `heading "${text}"`,
  selector: HEADINGS,
  text,
  textMatch: 'exact',
})

const paragraph = (text: string): EpubReaderElement => ({
  label: `paragraph "${text}"`,
  selector: 'p',
  text,
  textMatch: 'start',
})

/** The element is rendered exactly once and its span passes `check`. */
function expectEpubElementSpan(
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
function scrollEpubReaderUntilElementPassesTop(
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

export const bookReadingEpubMethods = () => ({
  expectEpubTextUsesScreenWidth() {
    ensureOnBookReadingPage()
    expectUsesScreenWidth('[data-testid="epub-book-viewer"] .epub-container')
    return this
  },
  expectEpubReadingViewShowsBookName(name: string) {
    ensureOnBookReadingPage()
    cy.get('[data-testid="book-reading-epub-global-bar-title"]').should(
      'contain',
      name
    )
    cy.get('[data-testid="epub-book-viewer"]').should('be.visible')
    return this
  },
  /**
   * epub.js renders inside iframes; the scrolled viewport is the inner `.epub-container`
   * (see epub.js Stage), not the Vue root `.epub-book-viewer-host`. Require the text to
   * intersect that container's on-screen rect (so content below the scroll position fails).
   */
  expectEpubContentTextVisible(text: string) {
    ensureOnBookReadingPage()
    cy.get('[data-testid="epub-book-viewer"]', { timeout: 30000 })
      .should('be.visible')
      .find('iframe')
      .should(($iframes) => {
        const hasText = [...$iframes].some((f) =>
          (f.contentDocument?.body?.innerText ?? '').includes(text)
        )
        expect(hasText, 'EPUB iframe should contain fixture text').to.be.true
      })
    cy.get(EPUB_READER_VIEW)
      .should('be.visible')
      .should(($host) => {
        const host = $host.get(0) as HTMLElement
        if (!epubHostViewportIntersectsMarker(host, text)) {
          for (const f of host.querySelectorAll('iframe')) {
            const doc = (f as HTMLIFrameElement).contentDocument
            if (!doc?.body?.innerText?.includes(text)) {
              continue
            }
            let best: HTMLElement | undefined
            let bestLen = Number.POSITIVE_INFINITY
            for (const node of doc.body.querySelectorAll('*')) {
              const e = node as HTMLElement
              const t = e.textContent ?? ''
              if (!t.includes(text) || t.length > bestLen) {
                continue
              }
              bestLen = t.length
              best = e
            }
            best?.scrollIntoView({ block: 'center', inline: 'nearest' })
            break
          }
        }
        expect(
          epubHostViewportIntersectsMarker(host, text),
          `EPUB text should intersect reader host viewport ("${text}")`
        ).to.be.true
      })
    return this
  },
  /** Clicks a link inside the book as a reader would; epub.js handles the click. */
  followEpubLinkInReader(linkText: string) {
    this.expectEpubContentTextVisible(linkText)
    cy.get(EPUB_READER_VIEW).then(($c) => {
      const [link] = epubReaderElementsWithText(
        $c.get(0) as HTMLElement,
        'a',
        linkText
      )
      expect(link, `EPUB link "${linkText}"`).to.exist
      link?.element.click()
    })
    return this
  },
  /**
   * The heading's top edge sits at the top edge of the reader's scrolled view
   * (`.epub-container`), within a few pixels.
   */
  expectEpubHeadingAtTopOfReader(headingText: string) {
    expectEpubElementSpan(heading(headingText), ({ top }) =>
      expect(
        Math.abs(top),
        `EPUB heading "${headingText}" should be at the top of the reader, but its top is ${top}px from the reader's top`
      ).to.be.at.most(TOP_TOLERANCE_PX)
    )
    return this
  },
  /**
   * The paragraph (found by its opening text) crosses the top edge of the reader's scrolled
   * view: its top is at or above the reader top and its bottom below it (within a few pixels).
   */
  expectEpubParagraphAtTopOfReader(paragraphText: string) {
    expectEpubElementSpan(
      paragraph(paragraphText),
      ({ top, bottom }) =>
        expect(
          top <= TOP_TOLERANCE_PX && bottom > TOP_TOLERANCE_PX,
          `EPUB paragraph "${paragraphText}" should cross the top of the reader, but it spans ${top}px to ${bottom}px from the reader's top`
        ).to.be.true
    )
    return this
  },
  scrollEpubReaderUntilHeadingPassesTop(headingText: string) {
    return scrollEpubReaderUntilElementPassesTop(heading(headingText), 10)
  },
  /**
   * Parks the paragraph's top just above the reader top, so the reading place saved from
   * this view points into the paragraph's first line.
   */
  scrollEpubReaderUntilParagraphIsAtTop(paragraphText: string) {
    return scrollEpubReaderUntilElementPassesTop(paragraph(paragraphText), 3)
  },
  /**
   * Navigate away via the GlobalBar "Notebook" link, wait for the pending reading-position
   * PATCH to flush so the server reflects the user's last position, then revisit the same
   * reading-page URL to force a full remount of BookReadingEpubView. `whileAway` runs after
   * the flush and before the remount.
   */
  leaveEpubReadingViewAndReturn(
    whileAway: (notebookId: string) => void = () => undefined
  ) {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="epub-book-viewer"]').should('be.visible')
    cy.location('pathname').then((pathname) => {
      const notebookId = notebookIdFromBookReadingPathname(String(pathname))
      cy.wait(2000)
      cy.contains('a', 'Notebook').click()
      cy.location('pathname').should('not.match', BOOK_READING_PATHNAME)
      whileAway(notebookId)
      router().visitNamed('bookReading', { notebookId })
      waitUntilAppIsNotBusy()
      cy.get('[data-testid="epub-book-viewer"]', {
        timeout: 30000,
      }).should('be.visible')
      cy.wait(1500)
    })
    return this
  },
  /** The stored exact place is replaced by one with no spine item; href and fragment stay. */
  leaveEpubReadingViewAndReturnAfterItsExactPlaceStopsResolving() {
    return this.leaveEpubReadingViewAndReturn((notebookId) => {
      const url = `${e2eAppBaseUrl()}/api/notebooks/${notebookId}/book/reading-position`
      cy.request(url).then(({ body }) => {
        cy.request('PATCH', url, {
          locator: { ...body.locator, cfi: 'epubcfi(/6/200!/4/2/1:0)' },
        })
      })
    })
  },
  expectBookLayoutBlockEpubStartHrefContains(title: string, substring: string) {
    waitUntilAppIsNotBusy()
    cy.location('pathname').should('match', BOOK_READING_PATHNAME)
    bookBlockRowByTitle(title)
      .invoke('attr', 'data-epub-start-href')
      .should('include', substring)
    return this
  },
  expectEpubReadingControlPanelContentAnchored() {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="book-reading-reading-control-panel"]', {
      timeout: 10000,
    })
      .should('be.visible')
      .and('have.attr', 'data-panel-placement', 'anchored')
    return this
  },
})
