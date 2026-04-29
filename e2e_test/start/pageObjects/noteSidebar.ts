import { submittableForm } from '../forms'
import { pageIsNotLoading } from '../pageBase'
import noteCreationForm from './noteForms/noteCreationForm'

const sidebarAddChildButton = () => {
  const getButton = () =>
    cy.get('aside').findByRole('button', { name: 'Add Child Note' })
  return {
    click: () => {
      getButton().click()
      return { ...submittableForm }
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
        cy.findByText(noteTopology)
          .parent()
          .parent()
          .within(() => {
            cy.findByTitle('expand children').click()
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
    addingChildNoteButton() {
      pageIsNotLoading()
      return sidebarAddChildButton()
    },
    addingChildNote() {
      sidebarAddChildButton().click()
      return noteCreationForm
    },
  }
}
