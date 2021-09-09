/// <reference types="cypress" />

declare namespace Cypress {
  interface Chainable {
    cleanDBAndSeedData(): Chainable<any>;
    failure(): void;
    findNoteCardButton(
      noteTitle: string,
      btnTextOrTitle: string,
    ): Chainable<any>;
    findNoteCardEditButton(noteTitle: string): Chainable<any>;
    getFormControl(label: string): Chainable<any>;
    loginAs(username: string): Chainable<any>;
    logout(username?: string): Chainable<any>;
    pageIsLoaded(): Chainable<any>;
    subscribeToNote(
      noteTitle: string,
      dailyLearningCount: number,
    ): Chainable<any>;
    unsubscribeFromNotebook(noteTitle: string): Chainable<any>;
    updateCurrentUserSettingsWith(hash: any): Chainable<any>;
    visitMyNotebooks(noteTitle?: string): Chainable<any>;
  }
}
