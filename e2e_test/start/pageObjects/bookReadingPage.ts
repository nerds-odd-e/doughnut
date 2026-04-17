import { pageIsNotLoading } from '../pageBase'

export type BookLayoutRow = { depth: number; title: string }

/** Same geometry rule as `expectEpubContentTextVisible` (marker must intersect `.epub-container`). */
function epubHostViewportIntersectsMarker(
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
  return hOverlap > 8 && wOverlap > 8
}

const bookReadingPage = () => {
  const bookBlockRows = () =>
    cy
      .get('[data-testid="book-reading-book-layout"]')
      .find('[data-testid="book-reading-book-block"]')

  /** Row is the block button; `.contains(text)` alone would match the inner title span. */
  const bookBlockRowByTitle = (title: string) =>
    cy
      .get('[data-testid="book-reading-book-layout"]')
      .contains('[data-testid="book-reading-book-block"]', title)

  const assertPdfCanvasHasDarkPixels = (el: HTMLCanvasElement) => {
    expect(el.width, 'PDF canvas should have width').to.be.greaterThan(0)
    const ctx = el.getContext('2d')
    if (!ctx) throw new Error('No 2d context on PDF canvas')
    const w = el.width
    const h = el.height
    const data = ctx.getImageData(0, 0, w, h).data
    let darkPixels = 0
    const stride = 6
    for (let y = 0; y < h; y += stride) {
      for (let x = 0; x < w; x += stride) {
        const i = (y * w + x) * 4
        if ((data[i] ?? 255) < 128 && (data[i + 3] ?? 0) > 128) darkPixels++
      }
    }
    expect(
      darkPixels,
      'PDF canvas should have dark pixels (text rendered)'
    ).to.be.greaterThan(0)
  }

  const ocrPngBase64 = (base64: string) =>
    cy.task('ocrCanvasImage', base64, { timeout: 60000 })

  return {
    expectEpubReadingViewShowsBookName(name: string) {
      pageIsNotLoading()
      cy.location('pathname').should('match', /^\/d\/notebooks\/\d+\/book$/)
      cy.get('[data-testid="book-reading-page"]').should('exist')
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
      pageIsNotLoading()
      cy.location('pathname').should('match', /^\/d\/notebooks\/\d+\/book$/)
      cy.get('[data-testid="book-reading-page"]').should('exist')
      cy.get('[data-testid="epub-book-viewer"]', { timeout: 30000 })
        .should('be.visible')
        .find('iframe')
        .should(($iframes) => {
          const hasText = [...$iframes].some((f) =>
            (f.contentDocument?.body?.innerText ?? '').includes(text)
          )
          expect(hasText, 'EPUB iframe should contain fixture text').to.be.true
        })
        .root()
        .get('[data-testid="epub-book-viewer"] .epub-container')
        .should('be.visible')
        .should(($host) => {
          const host = $host.get(0) as HTMLElement
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
      pageIsNotLoading()
      cy.location('pathname').should('match', /^\/d\/notebooks\/\d+\/book$/)
      cy.get('[data-testid="book-reading-page"]').should('exist')
      cy.get('[data-testid="epub-book-viewer"]', { timeout: 30000 }).should(
        'be.visible'
      )
      const scrollSel =
        '[data-testid="epub-book-viewer"] .epub-book-viewer-host'
      const viewportSel = '[data-testid="epub-book-viewer"] .epub-container'
      const maxSteps = 48
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
     * Navigate away from the EPUB reading view via the GlobalBar "Notebook" link, then
     * revisit the same reading-page URL. Forces a full remount of BookReadingEpubView so
     * any saved reading position is re-fetched on return.
     */
    /**
     * Navigate away from the EPUB reading view via the GlobalBar "Notebook" link, then
     * revisit the same reading-page URL. Waits for the pending reading-position PATCH to
     * flush before reloading so the server state reflects the user's last position.
     */
    leaveEpubReadingViewAndReturn() {
      pageIsNotLoading()
      cy.get('[data-testid="epub-book-viewer"]').should('be.visible')
      cy.intercept('PATCH', '/api/notebooks/*/book/reading-position').as(
        'patchReadingPosition'
      )
      cy.location('pathname')
        .should('match', /^\/d\/notebooks\/\d+\/book$/)
        .then((pathname) => {
          const readingPath = pathname as unknown as string
          cy.contains('a', 'Notebook').click()
          cy.location('pathname').should(
            'not.match',
            /^\/d\/notebooks\/\d+\/book$/
          )
          cy.wait('@patchReadingPosition', { timeout: 5000 })
          cy.visit(readingPath)
          pageIsNotLoading()
          cy.get('[data-testid="epub-book-viewer"]', {
            timeout: 30000,
          }).should('be.visible')
        })
      return this
    },
    expectBookLayoutRows(expected: BookLayoutRow[]) {
      pageIsNotLoading()
      cy.location('pathname').should('match', /^\/d\/notebooks\/\d+\/book$/)
      cy.get('[data-testid="book-reading-page"]').should('exist')
      bookBlockRows()
        .should('have.length', expected.length)
        .each(($el, i) => {
          const row = expected[i]!
          cy.wrap($el)
            .should('have.attr', 'data-book-block-depth', String(row.depth))
            .and('contain', row.title)
          cy.wrap($el)
            .find('[data-testid="book-reading-book-block-guides"]')
            .should(
              'have.attr',
              'data-book-block-guide-depth',
              String(row.depth)
            )
          cy.wrap($el)
            .find('[data-testid="book-reading-book-block-guide"]')
            .should('have.length', row.depth)
        })
      return this
    },
    expectPdfBeginningVisible() {
      this.expectCurrentPage(1).expectVisibleOCRContains('Code Refactoring')
      return this
    },
    clickBookBlockByTitle(title: string) {
      pageIsNotLoading()
      cy.get('[data-testid="book-reading-page-indicator"]')
        .should('be.visible')
        .and('contain', ' /')
      bookBlockRowByTitle(title).click()
      bookBlockRowByTitle(title).should(
        'have.attr',
        'data-current-block',
        'true'
      )
      return this
    },
    /**
     * Book layout row click for flows without the PDF page indicator (no `book-reading-page-indicator`).
     * Does not assert layout state; use `expectBookBlockIsCurrentBlockByTitle` / selection steps as needed.
     */
    clickBookLayoutBlockByTitle(title: string) {
      pageIsNotLoading()
      cy.location('pathname').should('match', /^\/d\/notebooks\/\d+\/book$/)
      cy.get('[data-testid="book-reading-page"]').should('exist')
      bookBlockRowByTitle(title).click()
      return this
    },
    expectBookLayoutBlockEpubStartHrefContains(
      title: string,
      substring: string
    ) {
      pageIsNotLoading()
      cy.location('pathname').should('match', /^\/d\/notebooks\/\d+\/book$/)
      bookBlockRowByTitle(title)
        .invoke('attr', 'data-epub-start-href')
        .should('include', substring)
      return this
    },
    expectBookBlockIsCurrentSelectionByTitle(title: string) {
      pageIsNotLoading()
      bookBlockRowByTitle(title).should(
        'have.attr',
        'data-current-selection',
        'true'
      )
      bookBlockRows()
        .filter('[data-current-selection="true"]')
        .should('have.length', 1)
      return this
    },
    /**
     * No single layout row is both `data-current-selection` and `data-current-block` (reading
     * position has moved away from the explicit selection).
     */
    expectBookLayoutCurrentBlockDiffersFromSelection() {
      pageIsNotLoading()
      cy.get('[data-testid="book-reading-book-layout"]')
        .find('[data-current-selection="true"][data-current-block="true"]')
        .should('not.exist')
      bookBlockRows()
        .filter('[data-current-block="true"]')
        .should('have.length', 1)
      return this
    },
    expectBookBlockIsFocusedByTitle(title: string) {
      pageIsNotLoading()
      bookBlockRowByTitle(title).should('be.focused')
      return this
    },
    expectBookBlockAtDepth(title: string, depth: number) {
      pageIsNotLoading()
      bookBlockRowByTitle(title).should(
        'have.attr',
        'data-book-block-depth',
        String(depth)
      )
      return this
    },
    pressTabOnBookLayout() {
      cy.focused().trigger('keydown', {
        key: 'Tab',
        code: 'Tab',
        keyCode: 9,
        which: 9,
        bubbles: true,
        getModifierState: () => false,
      })
      return this
    },
    pressShiftTabOnBookLayout() {
      cy.focused().trigger('keydown', {
        key: 'Tab',
        code: 'Tab',
        keyCode: 9,
        which: 9,
        shiftKey: true,
        bubbles: true,
        getModifierState: (key: string) => key === 'Shift',
      })
      return this
    },
    pressBackspaceOnBookLayout() {
      cy.focused().trigger('keydown', {
        key: 'Backspace',
        code: 'Backspace',
        keyCode: 8,
        which: 8,
        bubbles: true,
        getModifierState: () => false,
      })
      return this
    },
    expectBookBlockNotPresent(title: string) {
      pageIsNotLoading()
      cy.get('[data-testid="book-reading-book-layout"]').should(
        'not.contain',
        title
      )
      return this
    },
    expectCurrentPage(pageNumber: number) {
      pageIsNotLoading()
      cy.get('[data-testid="book-reading-page-indicator"]')
        .should('be.visible')
        .and('contain', `${pageNumber} /`)
      const afterPageCanvasInk = cy
        .get('[data-testid="pdf-book-viewer"]')
        .should('be.visible')
        .get(
          `[data-testid="pdf-book-viewer"] .pdfViewer .page[data-page-number="${pageNumber}"] canvas`
        )
        .first()
        .should(($canvas) => {
          assertPdfCanvasHasDarkPixels($canvas[0] as HTMLCanvasElement)
        })
      return {
        expectVisibleOCRContains(marker: string, screenshotKey?: string) {
          const name =
            screenshotKey ?? `book-reading-pdf-viewer-ocr-p${pageNumber}`
          return afterPageCanvasInk.then(() => {
            let screenshotPath = ''
            return cy
              .get('[data-testid="pdf-book-viewer"]')
              .screenshot(name, {
                log: false,
                overwrite: true,
                onBeforeScreenshot($el: JQuery<HTMLElement>) {
                  $el
                    .find('[data-testid="book-block-selection-bbox-highlight"]')
                    .css('opacity', '0')
                },
                onAfterScreenshot($el: JQuery<HTMLElement>, props) {
                  screenshotPath = props.path
                  $el
                    .find('[data-testid="book-block-selection-bbox-highlight"]')
                    .css('opacity', '')
                },
              })
              .then(() => {
                expect(
                  screenshotPath,
                  'Cypress element screenshot path'
                ).to.not.equal('')
                return cy.readFile(screenshotPath, 'base64', {
                  timeout: 30000,
                })
              })
              .then((base64) => ocrPngBase64(base64 as string))
              .then((text) => {
                expect(
                  text as string,
                  `OCR text from visible PDF book viewer (element screenshot); expect content from page ${pageNumber} in view`
                ).to.contain(marker)
              })
          })
        },
      }
    },
    scrollPdfBookReaderToBringPage2IntoPrimaryView() {
      pageIsNotLoading()
      const page2Sel =
        '[data-testid="pdf-book-viewer"] .pdfViewer .page[data-page-number="2"]'
      cy.get(page2Sel)
        .first()
        // @ts-expect-error Cypress ScrollIntoViewOptions omits DOM `block`
        .scrollIntoView({ block: 'start' })
      // Block 2.2 starts at y0=89/1000 normalized on page 2. Scroll that extra
      // amount so block 2.2 is at the container top, guaranteeing its y0 is below
      // the viewport midpoint even with very short test viewports (e.g. 1200×280).
      cy.get(page2Sel)
        .first()
        .then(($page) => {
          const pageHeight = ($page[0] as HTMLElement).getBoundingClientRect()
            .height
          const extra = Math.ceil((89 / 1000) * pageHeight)
          cy.get('[data-testid="pdf-book-viewer"]').then(($viewer) => {
            cy.get('[data-testid="pdf-book-viewer"]').scrollTo(
              0,
              ($viewer[0] as HTMLElement).scrollTop + extra
            )
          })
        })
      return this
    },
    /**
     * Scroll the PDF until the book layout row identified by `blockTitle` is the viewport-derived
     * current block (same DOM contract as expectBookBlockIsCurrentBlockByTitle).
     * For refactoring.pdf, §2.2 is anchored on page 2.
     */
    scrollPdfBookReaderToMakeBookBlockCurrent(blockTitle: string) {
      pageIsNotLoading()
      if (!blockTitle.includes('2.2 Refactoring as Strengthening')) {
        throw new Error(
          `scrollPdfBookReaderToMakeBookBlockCurrent: unsupported book block (add scroll strategy): ${blockTitle}`
        )
      }
      this.scrollPdfBookReaderToBringPage2IntoPrimaryView()
      cy.wait(200)
      this.expectBookBlockIsCurrentBlockByTitle(blockTitle)
      return this
    },
    /**
     * Scrolls by 42% of the rendered page-1 height from §1's click position (y≈204 MinerU),
     * giving total scroll ≈ 624 MinerU — past §2's bbox bottom (y1=608) so §2 scrolls above the
     * viewport, making §2.1 (y0=631) the first visible anchor and therefore the current block.
     */
    scrollPdfBookReaderDownWithinSamePageForNextBbox() {
      pageIsNotLoading()
      cy.get(
        '[data-testid="pdf-book-viewer"] .pdfViewer .page[data-page-number="1"]'
      )
        .first()
        .then(($page) => {
          const pageHeight = ($page[0] as HTMLElement).getBoundingClientRect()
            .height
          const deltaPx = Math.round(pageHeight * 0.42)
          cy.get('[data-testid="pdf-book-viewer"]').then(($el) => {
            const newTop = ($el[0] as HTMLElement).scrollTop + deltaPx
            cy.get('[data-testid="pdf-book-viewer"]').scrollTo(0, newTop)
          })
        })
      return this
    },
    expectBookBlockIsCurrentBlockByTitle(title: string) {
      pageIsNotLoading()
      bookBlockRowByTitle(title)
        .should('have.attr', 'data-current-block', 'true')
        .and('have.attr', 'aria-current', 'location')
      bookBlockRows()
        .filter('[data-current-block="true"]')
        .should('have.length', 1)
      return this
    },
    setBookReadingViewport(width: number, height: number) {
      cy.viewport(width, height)
      return this
    },
    /**
     * After PDF scroll updates the current block to a lower book block, the book layout aside should
     * scroll so that row is not clipped. Expects a short viewport so the list overflows.
     */
    expectCurrentBlockVisibleInBookLayoutAside(title: string) {
      this.expectBookBlockIsCurrentBlockByTitle(title)
      cy.get('[data-testid="book-reading-book-layout-aside"]').should(
        ($aside) => {
          expect(
            $aside[0]!.scrollTop,
            'book layout aside should have scrolled to reveal the current block'
          ).to.be.greaterThan(0)
        }
      )
      bookBlockRowByTitle(title).should('be.visible')
      return this
    },
    /**
     * Scrolls down the PDF viewer in increments until the Reading Control Panel appears,
     * asserting it contains the selected block title.
     */
    scrollPdfUntilReadingControlPanelVisible(selectedBlockTitle: string) {
      pageIsNotLoading()
      const step = 150
      const doScroll = (remaining: number): void => {
        if (remaining <= 0) return
        cy.get('[data-testid="book-reading-reading-control-panel"]').then(
          ($panel) => {
            if ($panel.length > 0 && $panel.is(':visible')) return
            cy.get('[data-testid="pdf-book-viewer"]').then(($viewer) => {
              const el = $viewer[0] as HTMLElement
              cy.get('[data-testid="pdf-book-viewer"]').scrollTo(
                0,
                el.scrollTop + step
              )
            })
            doScroll(remaining - 1)
          }
        )
      }
      doScroll(20)
      cy.get('[data-testid="book-reading-reading-control-panel"]', {
        timeout: 10000,
      })
        .should('be.visible')
        .and('contain', selectedBlockTitle)
      return this
    },
    /**
     * Reading Control Panel: bottom of PDF main pane.
     * Contract for production: data-testid book-reading-reading-control-panel + book-reading-mark-as-read.
     */
    markBookBlockAsReadInReadingControlPanel(blockTitle: string) {
      pageIsNotLoading()
      cy.get('[data-testid="book-reading-reading-control-panel"]')
        .should('be.visible')
        .and('contain', blockTitle)
      cy.get('[data-testid="book-reading-mark-as-read"]')
        .should('be.visible')
        .click()
      return this
    },
    /**
     * Book layout row marked as read: `data-direct-content-read="true"` plus success right border
     * and screen-reader “Marked as read” on the row.
     */
    expectBookBlockMarkedAsReadInBookLayout(title: string) {
      pageIsNotLoading()
      bookBlockRowByTitle(title).should(
        'have.attr',
        'data-direct-content-read',
        'true'
      )
      return this
    },
    expectCurrentBlockNavigationBar(title: string) {
      pageIsNotLoading()
      cy.get('[data-testid="current-block-navigation-bar"]', { timeout: 10000 })
        .should('be.visible')
        .and('contain', title)
      return this
    },
    expectCurrentBlockNavigationBarNotVisible() {
      pageIsNotLoading()
      cy.get('[data-testid="current-block-navigation-bar"]').should('not.exist')
      return this
    },
    clickReadFromHere() {
      pageIsNotLoading()
      cy.get('[data-testid="read-from-here"]').should('be.visible').click()
      return this
    },
    clickBackToSelected() {
      pageIsNotLoading()
      cy.get('[data-testid="back-to-selected"]').should('be.visible').click()
      return this
    },
    clickAiReorganizeLayout() {
      pageIsNotLoading()
      cy.get('[data-testid="book-reading-ai-reorganize-layout"]')
        .should('be.visible')
        .click()
      cy.get('[data-testid="book-reading-layout-full-busy"]', {
        timeout: 60000,
      }).should('not.exist')
      return this
    },
    expectReorganizationPreviewDialog() {
      pageIsNotLoading()
      cy.get('[data-testid="book-layout-reorganize-preview-dialog"]').should(
        'have.class',
        'daisy-modal-open'
      )
      cy.get('#book-layout-reorganize-preview-title').should(
        'contain',
        'Reorganize layout (preview)'
      )
      return this
    },
    expectReorganizationPreviewBlockSuggestedDepth(
      blockTitle: string,
      suggestedDepth: number
    ) {
      pageIsNotLoading()
      cy.get('[data-testid="book-layout-reorganize-preview-dialog"]')
        .should('have.class', 'daisy-modal-open')
        .within(() => {
          cy.contains(
            '[data-testid="book-layout-reorganize-preview-row"]',
            blockTitle
          )
            .should('be.visible')
            .and('have.attr', 'data-suggested-depth', String(suggestedDepth))
        })
      return this
    },
    confirmAiReorganizeSuggestion() {
      pageIsNotLoading()
      cy.get('[data-testid="book-layout-reorganize-preview-confirm"]')
        .should('be.visible')
        .click()
      pageIsNotLoading()
      return this
    },
    expectContentBlockBboxOverlaysVisible() {
      pageIsNotLoading()
      cy.get('[data-testid="pdf-book-viewer"]')
        .find('[data-testid="book-block-selection-bbox-highlight"]')
        .should('exist')
      return this
    },
    clickContentBlockBboxOverlay() {
      pageIsNotLoading()
      cy.get('[data-book-content-block-id]')
        .first()
        .trigger('click', { bubbles: true, force: true })
      return this
    },
    clickLongTextContentBlockBboxOverlay() {
      pageIsNotLoading()
      cy.get(
        '[data-derived-title-truncated="true"][data-book-content-block-id]'
      )
        .first()
        .scrollIntoView()
      cy.get(
        '[data-derived-title-truncated="true"][data-book-content-block-id]'
      )
        .first()
        .trigger('click', { bubbles: true, force: true })
      return this
    },
    expectNewBlockCallout() {
      pageIsNotLoading()
      cy.get('[data-testid="new-book-block-callout"]').should('be.visible')
      return this
    },
    confirmNewBlockCallout() {
      pageIsNotLoading()
      cy.get('[data-testid="new-book-block-callout-confirm"]')
        .should('be.visible')
        .click()
      return this
    },
    expectNewChildBlockInLayout() {
      pageIsNotLoading()
      cy.get('[data-testid="book-reading-book-layout"]')
        .find('[data-book-block-depth="1"]')
        .should('have.length.greaterThan', 0)
      return this
    },
    expectTitlePromptWithDefaultTitle() {
      pageIsNotLoading()
      cy.get('[data-testid="new-block-title-dialog"]').should('be.visible')
      cy.get('[data-testid="new-block-title-input"]').should(
        'not.have.value',
        ''
      )
      return this
    },
    confirmTitlePrompt() {
      pageIsNotLoading()
      cy.get('[data-testid="new-block-title-confirm"]')
        .should('be.visible')
        .click()
      return this
    },
  }
}

export default bookReadingPage
