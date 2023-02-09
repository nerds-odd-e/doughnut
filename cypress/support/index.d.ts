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
    addSiblingNoteButton(): Chainable<any>
    assertBlogPostInWebsiteByTitle(article: any): Chainable<any>
    associateNoteWithWikidataId(title: any, wikiID: any): Chainable<any>
    backendTimeTravelTo(day: number, hour: number): Chainable<Subject>
    backendTimeTravelRelativeToNow(hours: number): Chainable<Subject>
    cleanDBAndResetTestabilitySettings(): Chainable<Subject>
    cleanDownloadFolder(): Chainable<any>
    clickAddChildNoteButton(): Chainable<any>
    clickButtonOnCardBody(noteTitle: any, buttonTitle: any): Chainable<any>
    clickNotePageButton(noteTitle: any, btnTextOrTitle: any, forceLoadPage: any): Chainable<any>
    clickNotePageButtonOnCurrentPage(btnTextOrTitle: any): Chainable<any>
    clickNotePageMoreOptionsButton(noteTitle: any, btnTextOrTitle: any): Chainable<any>
    clickNotePageMoreOptionsButtonOnCurrentPage(btnTextOrTitle: any): Chainable<any>
    clickLinkNob(target: string): Chainable<any>
    clickRadioByLabel(labelText: any): Chainable<any>
    deleteNoteViaAPI(): Chainable<Subject>
    dialogDisappeared(): Chainable<any>
    distanceBetweenCardsGreaterThan(cards: any, note1: any, note2: any, min: any): Chainable<any>
    expectBreadcrumb(item: string, addChildButton: boolean = true): Chainable<any>
    expectExactLinkTargets(targets: any): Chainable<any>
    expectFieldErrorMessage(message: string, field: string): Chainable<any>
    expectNoteCards(expectedCards: any): Chainable<any>
    findNoteTitle(title: string): Chainable<any>
    findNoteDescriptionOnCurrentPage(title: string): Chainable<any>
    findCardTitle(title: string): Chainable<any>
    findWikiAssociationButton(): Chainable<any>
    expectALinkThatOpensANewWindowWithURL(url: string): Chainable<any>
    expectAMapTo(latitude: string, longitude: string): Chainable<any>
    findUserSettingsButton(userName: string): Chainable<any>
    failure(): Chainable<any>
    featureToggle(enabled: boolean): Chainable<Subject>
    findNoteCardButton(noteTitle: string, btnTextOrTitle: string): Chainable<any>
    findNoteCardEditButton(noteTitle: string): Chainable<any>
    getFormControl(label: string): Chainable<any>
    getSeededNoteIdByTitle(noteTitle: string): Chainable<Subject>
    initialReviewInSequence(reviews: any): Chainable<any>
    initialReviewNotes(noteTitles: any): Chainable<any>
    initialReviewOneNoteIfThereIs({
      review_type,
      title,
      additional_info,
      skip,
    }: any): Chainable<any>
    inPlaceEdit(noteAttributes: any): Chainable<any>
    jumpToNotePage(noteTitle: any, forceLoadPage?: any): Chainable<any>
    loginAs(username: string): Chainable<any>
    logout(username?: string): Chainable<any>
    mock(): Chainable<Subject>
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
    randomizerAlwaysChooseLast(): Chainable<Subject>
    replaceFocusedTextAndEnter(test: any): Chainable<any>
    repeatReviewNotes(noteTitles: string): Chainable<any>
    restore(): Chainable<Subject>
    routerPush(fallback: any, name: any, params: any): Chainable<any>
    routerToReviews(): Chainable<any>
    routerToRoot(): Chainable<any>
    routerToInitialReview(): Chainable<any>
    routerToRepeatReview(): Chainable<any>
    routerToNotebooks(noteTitle?: string): Chainable<any>
    searchNote(searchKey: any, options: any): Chainable<any>
    seedNotes(seedNotes: unknown[], externalIdentifier?: any, circleName?: any): Chainable<Subject>
    seedLink(type: string, fromNoteTitle: string, toNoteTitle: string): Chainable<Subject>
    seedCircle(circleInfo: Record<string, string>): Chainable<Subject>
    selectViewOfNote(noteTitle: string, viewType: stirng): Chainable<any>
    shareToBazaar(noteTitle: string): Chainable<Subject>
    shouldSeeQuizWithOptions(questionParts: any, options: any): Chainable<any>
    startSearching(): Chainable<any>
    stubWikidataEntityLocation(wikidataId: string, lat: number, lng: number): Chainable<Subject>
    stubWikidataEntityBook(wikidataId: string, authorId: string[]): Chainable<Subject>
    stubWikidataEntityPerson(
      wikidataId: string,
      countryId: string,
      birthday: string,
    ): Chainable<Subject>
    stubWikidataEntityQuery(
      wikidataId: string,
      wikidataTitle: string,
      wikipediaLink: string | undefined,
    ): Chainable<Subject>
    stubWikidataSearchResult(wikidataLabel: string, wikidataId: string): Chainable<Subject>
    stubOpenAiCompletion(title: string, reply: string): Chainable<Subject>
    responseAsIfTheTokenIs(tokenValidity: string): Chainable<any>
    subscribeToNotebook(notebookTitle: string, dailyLearningCount: string): Chainable<any>
    submitNoteFormWith(noteAttributes: any): Chainable<any>
    submitNoteFormsWith(notes: any): Chainable<any>
    submitNoteCreationFormWith(noteAttributes: any): Chainable<any>
    submitNoteCreationFormSuccessfully(noteAttributes: any): Chainable<any>
    createNotebookWith(notes: any): Chainable<any>
    testability(): Chainable<any>
    timeTravelTo(day: number, hour: number): Chainable<Subject>
    triggerException(): Chainable<Subject>
    undoLast(undoThpe: string): Chainable<any>
    unsubscribeFromNotebook(noteTitle: string): Chainable<any>
    updateCurrentUserSettingsWith(hash: Record<string, string>): Chainable<Subject>
    setServiceUrl(serviceName: string, serviceUrl: string): Chainable<any>
    wikidataService(): Chainable<any>
    openAiService(): Chainable<any>
    withinMindmap(): Chainable<any>
    yesIRemember(): Chainable<any>
  }
}
