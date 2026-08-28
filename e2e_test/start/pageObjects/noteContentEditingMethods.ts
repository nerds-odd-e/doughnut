import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  findNoteContentRegion,
  noteContentRegion,
} from './notePageContentRegion'
import { toolbarButton } from './toolbarButton'

export const noteContentEditingMethods = () => ({
  switchToRichContent() {
    cy.get('body').then(($body) => {
      const toRich = $body.find('button[aria-label^="Edit as rich content"]')
      if (toRich.length > 0) {
        cy.wrap(toRich.first()).click()
      }
    })
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
    cy.get('textarea').clear().invoke('val', markdown).trigger('input')
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
