/* eslint-disable  @typescript-eslint/no-explicit-any */
/* eslint-disable unused-imports/no-unused-imports */
/* eslint @typescript-eslint/no-unused-vars: 0 */
/// <reference types="cypress" />
// @ts-check
declare namespace Cypress {
  interface IUserSetting {
    daily_new_notes_count?: number
    space_intervals?: number
  }
  interface Chainable<Subject = any> {
    cleanDownloadFolder(): Chainable<any>
    clickAddChildNoteButton(): Chainable<any>
    clickRadioByLabel(labelText: any): Chainable<any>
    expectNoteCards(expectedCards: any): Chainable<any>
    failure(): Chainable<any>
    findNoteCardButton(noteTitle: string, btnTextOrTitle: string): Chainable<any>
    findNoteCardEditButton(noteTitle: string): Chainable<any>
    getFormControl(label: string): Chainable<any>
    expectNoteTitle(title: string): Chainable<any>
    expectCurrentNoteDescription(expectedDescription: string): Chainable<any>
    inPlaceEdit(noteAttributes: any): Chainable<any>
    jumpToNotePage(noteTitle: any, forceLoadPage: any): Chainable<any>
    loginAs(username: string): Chainable<any>
    logout(username?: string): Chainable<any>
    navigateToChild(noteTitle: any): Chainable<any>
    navigateToNotePage(notePath: NotePath): Chainable<any>
    openAndSubmitNoteAccessoriesFormWith(noteTitle: string, NoteAccessoriesAttributes: Record<string, string>): Chainable<any>
    pageIsNotLoading(): Chainable<any>
    replaceFocusedText(test:any): Chainable<any>
    routerPush(fallback:any, name:any, params:any): Chainable<any>
    routerToReviews(): Chainable<any>
    routerToInitialReview(): Chainable<any>
    routerToRepeatReview(): Chainable<any>
    routerToNotebooks(noteTitle?: string): Chainable<any>
    subscribeToNotebook(notebookTitle: string, dailyLearningCount: number): Chainable<any>
    submitNoteFormWith(noteAttributes: any): Chainable<any>
    submitNoteFormsWith(notes: any): Chainable<any>
    submitNoteCreationFormWith(noteAttributes: any): Chainable<any>
    submitNoteCreationFormsWith(notes: any): Chainable<any>
    unsubscribeFromNotebook(noteTitle: string): Chainable<any>
  }
}
