import { NoteForm } from './note_form'

export class NotePage {
  addChildNote() {
    cy.get('[data-cy="add-child-note-button"]').click()
    return new NoteForm()
  }

  fillInTitle(title: string) {
    cy.get('[data-cy="note-title-input"]').clear().type(title)
    return this
  }

  save() {
    cy.get('[data-cy="save-note-button"]').click()
    return new NotePage()
  }
}
