import noteCreationForm from './forms/noteCreationForm'

export const sidebarToolbarButton = (
  selector: () => Cypress.Chainable<JQuery<HTMLElement>>
) => {
  return {
    click: () => {
      selector().click({ force: true })
      return noteCreationForm
    },
    shouldNotExist: () =>
      selector().should(($el) => {
        expect(
          $el.length,
          'Expected notes to be read-only (no note creation control), but the control was present'
        ).to.equal(0)
      }),
  }
}

export const sidebarAddNoteButton = () =>
  sidebarToolbarButton(() =>
    cy.get('aside [data-testid="note-creation-new-button"] button')
  )

export const sidebarAddFolderButton = () =>
  sidebarToolbarButton(() =>
    cy.get('aside').findByRole('button', { name: 'New folder' })
  )
