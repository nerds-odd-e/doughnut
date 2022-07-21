/* eslint-disable  @typescript-eslint/no-explicit-any */
/* eslint @typescript-eslint/no-unused-vars: 0 */
/// <reference types="cypress" />
// @ts-check
declare namespace Cypress {
  interface UserSetting {
    daily_new_notes_count?: number
    space_intervals?: number
  }
  interface Chainable<Subject = any> {
    assertBlogPostInWebsiteByTitle(article: any): Chainable<any>
    backendTimeTravelTo(testability: TestabilityHelper, day: number, hour: number): Chainable<any>
    backendTimeTravelRelativeToNow(testability: TestabilityHelper, hours: number): Chainable<any>
    cleanDBAndResetTestabilitySettings(testability: TestabilityHelper): Chainable<any>
    cleanDownloadFolder(): Chainable<any>
    clickAddChildNoteButton(): Chainable<any>
    clickAssociateWikiDataButton(title: any, wikiID: any): Chainable<any>
    clickButtonOnCardBody(noteTitle: any, buttonTitle: any): Chainable<any>
    clickNotePageButton(noteTitle: any, btnTextOrTitle: any, forceLoadPage: any): Chainable<any>
    clickNotePageMoreOptionsButton(noteTitle: any, btnTextOrTitle: any): Chainable<any>
    clickNotePageMoreOptionsButtonOnCurrentPage(btnTextOrTitle: any): Chainable<any>
    clickNoteToolbarButton(btnTextOrTitle: any): Chainable<any>
    clickLinkNob(target: string): Chainable<any>
    clickRadioByLabel(labelText: any): Chainable<any>
    deleteNoteViaAPI(subject: any): Chainable<any>
    distanceBetweenCardsGreaterThan(cards: any, note1: any, note2: any, min: any): Chainable<any>
    expectCurrentNoteDescription(expectedDescription: string): Chainable<any>
    expectExactLinkTargets(targets: any): Chainable<any>
    expectFieldErrorMessage(message: string): Chainable<any>
    expectNoteCards(expectedCards: any): Chainable<any>
    expectNoteTitle(title: string): Chainable<any>
    failure(): Chainable<any>
    featureToggle(testability: TestabilityHelper, enabled: boolean): Chainable<any>
    findNoteCardButton(noteTitle: string, btnTextOrTitle: string): Chainable<any>
    findNoteCardEditButton(noteTitle: string): Chainable<any>
    getFormControl(label: string): Chainable<any>
    getSeededNoteIdByTitle(testability: TestabilityHelper, noteTitle: string): Chainable<any>
    initialReviewInSequence(reviews: any): Chainable<any>
    initialReviewNotes(noteTitles: any): Chainable<any>
    initialReviewOneNoteIfThereIs({
      review_type,
      title,
      additional_info,
      skip,
    }: any): Chainable<any>
    inPlaceEdit(noteAttributes: any): Chainable<any>
    jumpToNotePage(noteTitle: any, forceLoadPage: any): Chainable<any>
    loginAs(username: string): Chainable<any>
    logout(username?: string): Chainable<any>
    mock(wikidataServiceTester: WikidataServiceTester): Chainable<any>
    navigateToChild(noteTitle: any): Chainable<any>
    navigateToCircle(circleName: any): Chainable<any>
    navigateToNotePage(notePath: NotePath): Chainable<any>
    noteByTitle(noteTitle: string): Chainable<any>
    openAndSubmitNoteAccessoriesFormWith(
      noteTitle: string,
      NoteAccessoriesAttributes: Record<string, string>,
    ): Chainable<any>
    openCirclesSelector(): Chainable<any>
    pageIsNotLoading(): Chainable<any>
    randomizerAlwaysChooseLast(testatbility: TestabilityHelper): Chainable<any>
    replaceFocusedText(test: any): Chainable<any>
    repeatReviewNotes(noteTitles: string): Chainable<any>
    restore(wikidataServiceTester: WikidataServiceTester): Chainable<any>
    routerPush(fallback: any, name: any, params: any): Chainable<any>
    routerToReviews(): Chainable<any>
    routerToRoot(): Chainable<any>
    routerToInitialReview(): Chainable<any>
    routerToRepeatReview(): Chainable<any>
    routerToNotebooks(noteTitle?: string): Chainable<any>
    searchNote(searchKey: any, options: any): Chainable<any>
    seedNotes(
      testability: TestabilityHelper,
      seedNotes: unknown[],
      externalIdentifier: any,
      circleName: any,
    ): Chainable<any>
    seedLink(
      testability: TestabilityHelper,
      seedNotes: unknown[],
      externalIdentifier: any,
      circleName: any,
    ): Chainable<any>
    seedCircle(testability: TestabilityHelper, circleInfo: Record<string, string>): Chainable<any>
    shareToBazaar(testability: TestabilityHelper, noteTitle: string): Chainable<any>
    shouldSeeQuizWithOptions(questionParts: any, options: any): Chainable<any>
    startSearching(): Chainable<any>
    stubWikidataEntityQuery(
      wikidataServiceTester: WikidataServiceTester,
      wikidataId: string,
      wikidataTitle: string,
      wikipediaLink: string,
    ): Chainable<any>
    stubWikidataSearchResult(
      wikidataServiceTester: WikidataServiceTester,
      wikidataLabel: string,
      wikidataId: string,
    ): Chainable<any>
    subscribeToNotebook(notebookTitle: string, dailyLearningCount: string): Chainable<any>
    submitNoteFormWith(noteAttributes: any): Chainable<any>
    submitNoteFormsWith(notes: any): Chainable<any>
    submitNoteCreationFormWith(noteAttributes: any): Chainable<any>
    submitNoteCreationFormsWith(notes: any): Chainable<any>
    testability(): Chainable<any>
    timeTravelTo(testability: TestabilityHelper, day: number, hour: number): Chainable<any>
    triggerException(testatbility: TestabilityHelper): Chainable<any>
    unsubscribeFromNotebook(noteTitle: string): Chainable<any>
    updateCurrentUserSettingsWith(testability: TestabilityHelper, hash: Record<string, string>): Chainable<any>
    wikidataService(): Chainable<any>
    withinMindmap(): Chainable<any>
    yesIRemember(): Chainable<any>
  }
}
