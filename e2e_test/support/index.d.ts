/* eslint-disable  @typescript-eslint/no-explicit-any */
/* eslint @typescript-eslint/no-unused-vars: 0 */
/// <reference types="cypress" />
// @ts-check
declare namespace Cypress {
  interface Chainable<Subject = any> {
    dismissLastErrorMessage(): Chainable<any>
    cleanDownloadFolder(): Chainable<any>
    clickButtonOnCardBody(noteTopology: any, buttonTitle: any): Chainable<any>
    clickRadioByLabel(labelText: any): Chainable<any>
    expectBreadcrumb(
      item: string,
      addChildButton: boolean = true
    ): Chainable<any>
    expectFieldErrorMessage(field: string, message: string): Chainable<any>
    expectNoteCards(expectedCards: any): Chainable<any>
    findCardTitle(title: string): Chainable<any>
    expectAMapTo(latitude: string, longitude: string): Chainable<any>
    failure(): Chainable<any>
    formField(label: string): Chainable<any>
    assignFieldValue(value: string): Chainable<any>
    fieldShouldHaveValue(value: string): Chainable<any>
    noteByTitle(noteTopology: string): Chainable<any>
    pageIsNotLoading(): Chainable<any>
    clearFocusedText(): Chainable<any>
    replaceFocusedTextAndEnter(test: any): Chainable<any>
    routerPush(fallback: any, name: any, params: any): Chainable<any>
    routerToRoot(): Chainable<any>
    yesIRemember(): Chainable<any>
  }
}
