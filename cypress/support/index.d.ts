/// <reference types="cypress" />

declare namespace Cypress {
  interface userSetting {
    daily_new_notes_count?: number;
    space_intervals?: number;
  }
  interface Chainable {
    cleanDBAndSeedData(): void;
    failure(): void;
    findNoteCardButton(
      noteTitle: string,
      btnTextOrTitle: string,
    ): Chainable<Element>;
    findNoteCardEditButton(noteTitle: string): Chainable<Element>;
    getFormControl(label: string): Chainable<Element>;
    expectNoteTitle(title: string): void;
    loginAs(username: string): void;
    logout(username?: string): void;
    pageIsLoaded(): void;
    subscribeToNote(
      noteTitle: string,
      dailyLearningCount: number,
    ): Chainable<Element>;
    triggerException(): void;
    unsubscribeFromNotebook(noteTitle: string): Chainable<Element>;
    updateCurrentUserSettingsWith(settings: userSetting): Chainable<Element>;
    visitMyNotebooks(noteTitle?: string): Chainable<Element>;
  }
}
