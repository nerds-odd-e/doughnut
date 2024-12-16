import { NotePage } from './note_page'

export class NoteForm {
  fillInTitle(title: string) {
    cy.get('[data-cy="note-title-input"]').clear().type(title)
    return this
  }

  save() {
    cy.get('[data-cy="save-note-button"]').click()
    return new NotePage()
  }
}
