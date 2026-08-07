import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  bookBlockRowByTitle,
  ensureOnBookReadingPage,
  epubHostViewportIntersectsMarker,
} from './bookReadingShared'

export const bookReadingEpubMethods = () => ({
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
    cy.get('[data-testid="epub-book-viewer"] .epub-container')
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
    const scrollSel = '[data-testid="epub-book-viewer"] .epub-container'
    const viewportSel = '[data-testid="epub-book-viewer"] .epub-container'
    const maxSteps = 96
    const step = (n: number): Cypress.Chainable =>
      cy.get(scrollSel).then(($scrollEl) => {
        const scrollEl = $scrollEl.get(0) as HTMLElement
        return cy.get(viewportSel).then(($vp) => {
          const viewport = $vp.get(0) as HTMLElement
          if (epubHostViewportIntersectsMarker(viewport, markerText)) {
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
          if (
            nextTop <= scrollEl.scrollTop &&
            scrollEl.scrollTop >= maxTop - 1
          ) {
            throw new Error(
              `scrollEpubReaderUntilTextInViewport: scroll exhausted without "${markerText}" in viewport`
            )
          }
          cy.wrap(scrollEl).scrollTo(0, nextTop)
          cy.wait(200)
          return step(n + 1)
        })
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
    cy.get('[data-testid="epub-book-viewer"] .epub-container').then(($c) => {
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
    cy.location('pathname')
      .should('match', /^\/notebooks\/\d+\/book$/)
      .then((pathname) => {
        const readingPath = pathname as unknown as string
        cy.wait(2000)
        cy.contains('a', 'Notebook').click()
        cy.location('pathname').should('not.match', /^\/notebooks\/\d+\/book$/)
        cy.visit(readingPath)
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
    cy.location('pathname').should('match', /^\/notebooks\/\d+\/book$/)
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
