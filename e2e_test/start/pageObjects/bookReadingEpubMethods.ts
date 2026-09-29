import { waitUntilAppIsNotBusy } from '../pageBase'
import router from '../router'
import {
  BOOK_READING_PATHNAME,
  bookBlockRowByTitle,
  ensureOnBookReadingPage,
  epubHeadingOffsetsFromReaderTopPx,
  epubHostViewportIntersectsMarker,
  expectUsesScreenWidth,
  epubReaderElementsWithText,
  notebookIdFromBookReadingPathname,
} from './bookReadingShared'

/** The reader's scrolled view: epub.js's stage inside the viewer. */
const EPUB_READER_VIEW = '[data-testid="epub-book-viewer"] .epub-container'

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
    ensureOnBookReadingPage()
    const tolerancePx = 8
    cy.get(EPUB_READER_VIEW, { timeout: 30000 }).should(($c) => {
      const offsets = epubHeadingOffsetsFromReaderTopPx(
        $c.get(0) as HTMLElement,
        headingText
      )
      expect(offsets, `EPUB heading "${headingText}" rendered`).to.have.length(
        1
      )
      const offset = Math.round(offsets[0] ?? Number.NaN)
      expect(
        Math.abs(offset),
        `EPUB heading "${headingText}" should be at the top of the reader, but its top is ${offset}px from the reader's top`
      ).to.be.at.most(tolerancePx)
    })
    return this
  },
  /**
   * Scrolls the reader down in steps until the heading is rendered, then scrolls it just
   * above the top of the reader's view.
   */
  scrollEpubReaderUntilHeadingPassesTop(headingText: string) {
    ensureOnBookReadingPage()
    const maxSteps = 48
    const step = (n: number): Cypress.Chainable =>
      cy.get(EPUB_READER_VIEW, { timeout: 30000 }).then(($c) => {
        const container = $c.get(0) as HTMLElement
        const [offset] = epubHeadingOffsetsFromReaderTopPx(
          container,
          headingText
        )
        if (offset !== undefined) {
          container.scrollTop += offset + 10
          cy.wait(300)
          return cy.wrap(null)
        }
        if (n >= maxSteps) {
          throw new Error(
            `scrollEpubReaderUntilHeadingPassesTop: heading "${headingText}" not rendered after ${maxSteps} steps`
          )
        }
        container.scrollTop += Math.ceil(container.clientHeight * 0.85)
        cy.wait(200)
        return step(n + 1)
      })
    return cy.then(() => step(0))
  },
  /**
   * Scrolls the epub.js host (`.epub-book-viewer-host`, `overflow-auto`) in steps until
   * marker text intersects `.epub-container` (same contract as `expectEpubContentTextVisible`),
   * so `relocated` can advance past the initially displayed spine item without a layout click.
   */
  scrollEpubReaderUntilTextInViewport(markerText: string) {
    ensureOnBookReadingPage()
    cy.get('[data-testid="epub-book-viewer"]', { timeout: 30000 }).should(
      'be.visible'
    )
    // epub.js listens for scroll on the inner stage `.epub-container` (not `.epub-book-viewer-host`).
    const maxSteps = 96
    const step = (n: number): Cypress.Chainable =>
      cy.get(EPUB_READER_VIEW).then(($scrollEl) => {
        const scrollEl = $scrollEl.get(0) as HTMLElement
        if (epubHostViewportIntersectsMarker(scrollEl, markerText)) {
          cy.wait(200)
          return cy.wrap(null)
        }
        if (n >= maxSteps) {
          throw new Error(
            `scrollEpubReaderUntilTextInViewport: exceeded ${maxSteps} steps without "${markerText}" in viewport`
          )
        }
        const maxTop = Math.max(
          0,
          scrollEl.scrollHeight - scrollEl.clientHeight
        )
        const chunk = Math.max(80, Math.ceil(scrollEl.clientHeight * 0.85))
        const nextTop = Math.min(scrollEl.scrollTop + chunk, maxTop)
        if (nextTop <= scrollEl.scrollTop && scrollEl.scrollTop >= maxTop - 1) {
          throw new Error(
            `scrollEpubReaderUntilTextInViewport: scroll exhausted without "${markerText}" in viewport`
          )
        }
        cy.wrap(scrollEl).scrollTo(0, nextTop)
        cy.wait(200)
        return step(n + 1)
      })
    return cy.then(() => step(0))
  },
  /**
   * Scrolls the epub.js `.epub-container` so the **current chapter** (iframe) is aligned
   * to the top of the stage — not `scrollTop = 0` (book beginning), so a follow-up
   * "scroll until text" can still cross fragment boundaries within the same spine file.
   */
  scrollEpubReaderHostToTop() {
    ensureOnBookReadingPage()
    cy.get('[data-testid="epub-book-viewer"]', { timeout: 30000 }).should(
      'be.visible'
    )
    cy.get(EPUB_READER_VIEW).then(($c) => {
      const container = $c.get(0) as HTMLElement
      for (const f of container.querySelectorAll('iframe')) {
        const doc = (f as HTMLIFrameElement).contentDocument
        const h1 = doc?.querySelector('h1')
        if (h1 && (h1.textContent ?? '').includes('Chapter Beta')) {
          h1.scrollIntoView({ block: 'start', inline: 'nearest' })
          return
        }
      }
      container.scrollTop = 0
    })
    cy.wait(200)
    return this
  },
  /**
   * Navigate away via the GlobalBar "Notebook" link, wait for the pending reading-position
   * PATCH to flush so the server reflects the user's last position, then revisit the same
   * reading-page URL to force a full remount of BookReadingEpubView.
   */
  leaveEpubReadingViewAndReturn() {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="epub-book-viewer"]').should('be.visible')
    cy.location('pathname').then((pathname) => {
      const notebookId = notebookIdFromBookReadingPathname(String(pathname))
      cy.wait(2000)
      cy.contains('a', 'Notebook').click()
      cy.location('pathname').should('not.match', BOOK_READING_PATHNAME)
      router().visitNamed('bookReading', { notebookId })
      waitUntilAppIsNotBusy()
      cy.get('[data-testid="epub-book-viewer"]', {
        timeout: 30000,
      }).should('be.visible')
      cy.wait(1500)
    })
    return this
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
