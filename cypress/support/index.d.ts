/// <reference types="cypress" />

declare namespace Cypress {
  interface Chainable {
    cleanDBAndSeedData(): Chainable<any>;
    findNoteCardButton(
      noteTitle: string,
      btnTextOrTitle: string,
    ): Chainable<any>;
    findNoteCardEditButton(noteTitle: string): Chainable<any>;
    getFormControl(label: string): Chainable<any>;
    subscribeToNote(
      noteTitle: string,
      dailyLearningCount: number,
    ): Chainable<any>;
    unsubscribeFromNotebook(noteTitle: string): Chainable<any>;
    visitMyNotebooks(noteTitle?: string): Chainable<any>;
  }
}
