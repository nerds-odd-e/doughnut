/* eslint-disable  @typescript-eslint/no-explicit-any */
/* eslint @typescript-eslint/no-unused-vars: 0 */
/// <reference types="cypress" />
// @ts-check
declare namespace Cypress {
  interface Chainable<Subject = any> {
    dismissLastErrorMessage(): Chainable<any>
    cleanDownloadFolder(): Chainable<any>
    clickButtonOnCardBody(noteTopic: any, buttonTitle: any): Chainable<any>
    clickRadioByLabel(labelText: any): Chainable<any>
    dialogDisappeared(): Chainable<any>
    expectBreadcrumb(
      item: string,
      addChildButton: boolean = true
    ): Chainable<any>
    expectFieldErrorMessage(field: string, message: string): Chainable<any>
    expectNoteCards(expectedCards: any): Chainable<any>
    findCardTitle(topic: string): Chainable<any>
    expectAMapTo(latitude: string, longitude: string): Chainable<any>
    failure(): Chainable<any>
    formField(label: string): Chainable<any>
    assignFieldValue(value: string): Chainable<any>
    fieldShouldHaveValue(value: string): Chainable<any>
    initialReviewInSequence(recalls: any): Chainable<any>
    initialReviewNotes(noteTopics: any): Chainable<any>
    initialReviewOneNoteIfThereIs({
      review_type,
      topic,
      additional_info,
      skip,
    }: any): Chainable<any>
    noteByTitle(noteTopic: string): Chainable<any>
    pageIsNotLoading(): Chainable<any>
    clearFocusedText(): Chainable<any>
    replaceFocusedTextAndEnter(test: any): Chainable<any>
    repeatReviewNotes(noteTopics: string): Chainable<any>
    goAndRepeatReviewNotes(noteTopics: string): Chainable<any>
    repeatMore(): Chainable<any>
    routerPush(fallback: any, name: any, params: any): Chainable<any>
    routerToReviews(): Chainable<any>
    routerToRoot(): Chainable<any>
    routerToInitialReview(): Chainable<any>
    routerToRepeatReview(): Chainable<any>
    shouldSeeQuizWithOptions(questionParts: any, options: any): Chainable<any>
    startSearching(): Chainable<any>
    undoLast(undoThpe: string): Chainable<any>
    yesIRemember(): Chainable<any>
  }
}
