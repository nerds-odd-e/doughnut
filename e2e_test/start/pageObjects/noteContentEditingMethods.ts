import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  findNoteContentRegion,
  noteContentRegion,
} from './notePageContentRegion'
import { toolbarButton } from './toolbarButton'

export function switchToRichContentIfNeeded() {
  cy.get('body').then(($body) => {
    const toRich = $body.find('button[aria-label^="Edit as rich content"]')
    if (toRich.length > 0) {
      cy.wrap(toRich.first()).click()
    }
  })
}

export const noteContentEditingMethods = () => ({
  expectStationaryToolbarDuringEditModeChanges() {
    cy.get('[data-note-toolbar]').then(($toolbar) => {
      const toolbar = $toolbar[0]!
      const scrollport = toolbar.parentElement!.parentElement!
      const command = toolbar.querySelector<HTMLElement>('[title]')!
      const win = toolbar.ownerDocument.defaultView!
      const initialLeft = command.getBoundingClientRect().left
      const positions: number[] = []
      const verticalScrollRanges: number[] = []
      const sampleFrames = () =>
        new Cypress.Promise<void>((resolve) => {
          let frames = 0
          const sample = () => {
            positions.push(command.getBoundingClientRect().left)
            const scrollsVertically = ['auto', 'scroll'].includes(
              win.getComputedStyle(scrollport).overflowY
            )
            verticalScrollRanges.push(
              scrollsVertically
                ? Math.max(0, scrollport.scrollHeight - scrollport.clientHeight)
                : 0
            )
            if (++frames === 12) resolve()
            else win.requestAnimationFrame(sample)
          }
          win.requestAnimationFrame(sample)
        })
      toolbarButton('Edit as markdown').click()
      cy.then(sampleFrames)
      toolbarButton('Edit as rich content').click()
      cy.then(sampleFrames)
      cy.then(() => {
        expect(
          { min: Math.min(...positions), max: Math.max(...positions) },
          'note toolbar position through both editing modes'
        ).to.deep.equal({
          min: initialLeft,
          max: initialLeft,
        })
        expect(
          Math.max(...verticalScrollRanges),
          'vertical scroll range of the horizontal note tools'
        ).to.equal(0)
      })
    })
    return this
  },
  switchToRichContent() {
    switchToRichContentIfNeeded()
    return this
  },
  flushPendingContentSave() {
    findNoteContentRegion().then(($noteField) => {
      const $textarea = $noteField.find('textarea').filter(':visible')
      if ($textarea.length) {
        cy.wrap($textarea.first()).blur()
      }
    })
    cy.get('body').click(0, 0, { force: true })
    cy.get('.dirty').should('not.exist')
    waitUntilAppIsNotBusy()
    return this
  },
  openMarkdownContentEditor() {
    cy.get('body').then(($body) => {
      const toMarkdown = $body.find('button[aria-label^="Edit as markdown"]')
      if (toMarkdown.length > 0) {
        cy.wrap(toMarkdown.first()).click()
      }
    })
    return this
  },
  expectMarkdownContentSourceContains(fragment: string) {
    cy.get('textarea').should(($ta) => {
      expect($ta.val()).to.include(fragment)
    })
    return this
  },
  expectMarkdownContentSourceDoesNotContain(fragment: string) {
    cy.get('textarea').should(($ta) => {
      expect($ta.val()).to.not.include(fragment)
    })
    return this
  },
  updateContentAsMarkdown(markdown: string) {
    toolbarButton('Edit as markdown').click()
    // Overflow on `.note-content-wrapper` can report the scroll parent as
    // covering this textarea; same force pattern as rich-property edits.
    findNoteContentRegion()
      .find('textarea')
      .filter(':visible')
      .first()
      .scrollIntoView()
      .clear({ force: true })
      .invoke('val', markdown)
      .trigger('input', { force: true })
    this.flushPendingContentSave()
    return this.switchToRichContent()
  },
  expectRenderedNoteContent(elements: Record<string, string>[]) {
    const kindToSelector: Record<string, string> = {
      'heading 1': 'h1',
      'heading 2': 'h2',
      'list item': 'li',
      'indented list item': 'li.ql-indent-1',
      table: 'table',
      'table header': 'th',
      'table cell': 'td',
      'wiki link': 'a',
      'dead wiki link': 'a.dead-wiki-link',
      'live wiki link': 'a.donut-wiki-link',
      'markdown link':
        'a:not(.donut-wiki-link):not(.dead-wiki-link):not(.pending-wiki-link)',
    }
    for (const element of elements) {
      const kind = (element.Kind ?? '').trim()
      const text = element.Text ?? ''
      const selector = kindToSelector[kind]
      expect(
        selector,
        `Unknown rendered note content kind "${kind}". Known: ${Object.keys(kindToSelector).join(', ')}`
      ).to.be.a('string')
      cy.get('#main-note-content .note-content .ql-editor').within(() => {
        if (text === '') {
          cy.get(selector!).should('exist')
        } else {
          cy.contains(selector!, text).should('exist')
        }
      })
    }
    return this
  },
  insertSoftLineBreakInContent(before: string, after: string) {
    cy.findByRole(noteContentRegion.role, {
      name: noteContentRegion.name,
    }).within(() => {
      cy.get('.ql-editor[contenteditable="true"], textarea').first().click()
    })
    cy.clearFocusedText().type(`${before}{shift}{enter}${after}`).blur()
    cy.get('.dirty').should('not.exist')
    waitUntilAppIsNotBusy()
    return this
  },
  editTextContent: (noteAttributes: Record<string, string>) => {
    for (const propName in noteAttributes) {
      const value = noteAttributes[propName]
      if (value) {
        if (propName === 'Content') {
          cy.findByRole(noteContentRegion.role, {
            name: noteContentRegion.name,
          }).within(() => {
            cy.get('.ql-editor[contenteditable="true"], textarea')
              .first()
              .click()
          })
        } else {
          cy.findByRole(propName.toLowerCase()).click()
        }
        const cypressState = cy as unknown as {
          state?: (key: string) => unknown
        }
        if (cypressState.state?.('clock')) {
          cy.tick(5000)
        }
        cy.clearFocusedText().type(value).blur()
        cy.get('.dirty').should('not.exist')
      }
    }
    waitUntilAppIsNotBusy()
  },
})
