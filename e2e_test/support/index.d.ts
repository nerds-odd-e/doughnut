/* eslint-disable  @typescript-eslint/no-explicit-any */
/* eslint @typescript-eslint/no-unused-vars: 0 */
/// <reference types="cypress" />
// @ts-check
declare namespace Cypress {
  interface Chainable<Subject = any> {
    cleanDownloadFolder(): Chainable<any>
    findCardTitle(title: string): Chainable<any>
    pageIsNotLoading(): Chainable<any>
    clearFocusedText(): Chainable<any>
  }
}
