import { pageIsNotLoading } from '../pageBase'
import noteCreationForm from './noteForms/noteCreationForm'

const sidebarAddNoteButton = (buttonName?: string) => {
  const getButton = () =>
    cy
      .get('aside')
      .findByRole('button', { name: buttonName ?? 'Add Child Note' })
  return {
    click: () => {
      getButton().click()
      return noteCreationForm
    },
    shouldNotExist: () => getButton().should('not.exist'),
  }
}

export const noteSidebar = () => {
  cy.findByRole('button', { name: 'toggle sidebar' }).then(($button) => {
    if (!$button.hasClass('sidebar-expanded')) {
      cy.wrap($button).click()
    }
  })
  cy.get('aside').should('be.visible')

  return {
    expand: (noteTopology: string) => {
      cy.get('aside').within(() => {
        cy.contains('.sidebar-folder-label', noteTopology)
          .closest('li')
          .then(($li) => {
            if ($li.attr('data-sidebar-folder-expanded') !== 'true') {
              cy.wrap($li).within(() => {
                cy.findByTitle('expand children').click()
              })
            }
          })
      })
    },
    siblingOrder: (higher: string, lower: string) => {
      cy.get('aside').within(() => {
        cy.contains(higher).parent().parent().nextAll().contains(lower)
      })
    },
    expectOrderedNotes(expectedNotes: Record<string, string>[]) {
      pageIsNotLoading()
      const expectedNoteTopics = expectedNotes.map((note) => note['note-title'])

      cy.get('aside ul li .title-text', { timeout: 15000 }).should(($els) => {
        const actualNotes = Array.from($els, (el) => el.innerText)

        expect(actualNotes.length, 'Number of notes should match').to.equal(
          expectedNoteTopics.length
        )

        actualNotes.forEach((actualNote, index) => {
          expect(actualNote, `Note at position ${index + 1}`).to.equal(
            expectedNoteTopics[index]
          )
        })
      })
    },
    addingNoteButton() {
      pageIsNotLoading()
      return sidebarAddNoteButton('Add note')
    },
    addingChildNoteButton() {
      pageIsNotLoading()
      return sidebarAddNoteButton()
    },
    addingChildNote() {
      sidebarAddNoteButton().click()
      return noteCreationForm
    },
    addingNextSiblingNote() {
      pageIsNotLoading()
      sidebarAddNoteButton('Add Next Sibling Note').click()
      return noteCreationForm
    },
    /** Expands a sidebar folder row by human **name** (not a note `.title-text` link). */
    navigateExpandFolder(folderLabel: string) {
      pageIsNotLoading()
      cy.get('aside').within(() => {
        cy.contains('.sidebar-folder-label', folderLabel)
          .closest('li')
          .then(($li) => {
            if ($li.attr('data-sidebar-folder-expanded') !== 'true') {
              cy.wrap($li).within(() => {
                cy.findByTitle('expand children').click()
              })
            }
          })
      })
      pageIsNotLoading()
    },
    navigateToNote(noteTopology: string) {
      pageIsNotLoading()
      cy.get('aside').within(() => {
        cy.findByText(noteTopology, { selector: '.title-text' })
          .should('be.visible')
          .click()
      })
      pageIsNotLoading()
    },
  }
}

/** Page objects that show the note sidebar (note page, notebook page) share these. */
export const sidebarChildNotePageMethods = () => ({
  addingNoteButton() {
    return noteSidebar().addingNoteButton()
  },
  addingChildNoteButton() {
    return noteSidebar().addingChildNoteButton()
  },
  addingChildNote() {
    return noteSidebar().addingChildNote()
  },
  addingNextSiblingNote() {
    return noteSidebar().addingNextSiblingNote()
  },
})
