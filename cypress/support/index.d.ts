/// <reference types="cypress" />
// @ts-check
declare namespace Cypress {
  interface IUserSetting {
    daily_new_notes_count?: number
    space_intervals?: number
  }
  interface IChainable {
    routerToReviews(): Chainable<Element>
    routerToInitialReview(): Chainable<Element>
    routerToRepeatReview(): Chainable<Element>
    cleanDBAndSeedData(): void
    enableFeatureToggle(enabled: boolean): void
    cleanDownloadFolder(): void
    timeTravelRelativeToNow(hours: number): void
    failure(): void
    findNoteCardButton(noteTitle: string, btnTextOrTitle: string): Chainable<Element>
    findNoteCardEditButton(noteTitle: string): Chainable<Element>
    getFormControl(label: string): Chainable<Element>
    expectNoteTitle(title: string): void
    expectCurrentNoteDescription(expectedDescription: string): void
    loginAs(username: string): void
    logout(username?: string): void
    pageIsNotLoading(): void
    subscribeToNote(noteTitle: string, dailyLearningCount: number): Chainable<Element>
    triggerException(): void
    unsubscribeFromNotebook(noteTitle: string): Chainable<Element>
    updateCurrentUserSettingsWith(settings: userSetting): Chainable<Element>
    visitMyNotebooks(noteTitle?: string): Chainable<Element>
  }
}
