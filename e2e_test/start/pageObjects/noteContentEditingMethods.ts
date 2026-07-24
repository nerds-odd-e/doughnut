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
  switchToRichContentMode() {
    return this.switchToRichContent()
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
  expectRichContent(elements: Record<string, string>[]) {
    for (const element of elements) {
      const tag = element.Tag as string
      const content = element.Content ?? ''
      cy.get('#main-note-content .note-content .ql-editor').within(() => {
        if (content === '') {
          cy.get(tag).should('exist')
        } else {
          cy.contains(tag, content).should('exist')
        }
      })
    }
  },
  editTextContent: (noteAttributes: Record<string, string>) => {
    const parseSpecialKeys = (text: string): string =>
      text.replace(/<Shift-Enter>/g, '{shift}{enter}')

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
        const parsedValue = parseSpecialKeys(value)
        cy.clearFocusedText().type(parsedValue).blur()
        cy.get('.dirty').should('not.exist')
      }
    }
    waitUntilAppIsNotBusy()
  },
})
