import { e2eAppBaseUrl } from '../../support/e2eAppUrl'
import { waitUntilAppIsNotBusy } from '../pageBase'
import router from '../router'
import {
  BOOK_READING_PATHNAME,
  bookBlockRowByTitle,
  ensureOnBookReadingPage,
  epubHostViewportIntersectsMarker,
  expectUsesScreenWidth,
  epubReaderElementsWithText,
  notebookIdFromBookReadingPathname,
} from './bookReadingShared'
import {
  EPUB_READER_VIEW,
  TOP_TOLERANCE_PX,
  expectEpubElementSpan,
  heading,
  paragraph,
  scrollEpubReaderUntilElementPassesTop,
} from './bookReadingEpubReaderElements'

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
   * Navigate away via the GlobalBar "Notebook" link, which flushes any pending reading-position
   * save, wait until every save the reader sent has been answered, then revisit the same
   * reading-page URL to force a full remount of BookReadingEpub. `whileAway` runs after
   * the saves and before the remount.
   */
  leaveEpubReadingViewAndReturn(
    whileAway: (notebookId: string) => void = () => undefined
  ) {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="epub-book-viewer"]').should('be.visible')
    let unansweredSaves = 0
    cy.intercept('PATCH', '**/book/reading-position', (req) => {
      unansweredSaves += 1
      req.on('after:response', () => {
        unansweredSaves -= 1
      })
    })
    cy.location('pathname').then((pathname) => {
      const notebookId = notebookIdFromBookReadingPathname(String(pathname))
      cy.contains('a', 'Notebook').click()
      cy.location('pathname').should('not.match', BOOK_READING_PATHNAME)
      waitUntilAppIsNotBusy()
      cy.wrap(null).should(() =>
        expect(unansweredSaves, 'unanswered reading-position saves').to.equal(0)
      )
      whileAway(notebookId)
      router().visitNamed('bookReading', { notebookId })
      waitUntilAppIsNotBusy()
      cy.get('[data-testid="epub-book-viewer"]', {
        timeout: 30000,
      }).should('be.visible')
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
