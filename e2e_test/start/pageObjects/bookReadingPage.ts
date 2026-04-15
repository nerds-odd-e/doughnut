import { pageIsNotLoading } from '../pageBase'

export type BookLayoutRow = { depth: number; title: string }

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
     * scroll so that row is not clipped (Phase 6.5). Expects a short viewport so the list overflows.
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
     * Reading Control Panel (Phase 2 reading record): bottom of PDF main pane.
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
    pressAndHoldOnContentBlockBboxOverlay() {
      pageIsNotLoading()
      cy.get('[data-book-content-block-id]')
        .first()
        .trigger('pointerdown', { button: 0, bubbles: true, force: true })
      cy.tick(600)
      return this
    },
    pressAndHoldOnLongTextContentBlockBboxOverlay() {
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
        .trigger('pointerdown', { button: 0, bubbles: true })
      cy.tick(600)
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
