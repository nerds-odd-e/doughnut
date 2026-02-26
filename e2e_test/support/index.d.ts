/* eslint-disable  @typescript-eslint/no-explicit-any */
/* eslint @typescript-eslint/no-unused-vars: 0 */
/// <reference types="cypress" />
// @ts-check
declare namespace Cypress {
  interface Chainable<Subject = any> {
    cleanDownloadFolder(): Chainable<any>
    clickRadioByLabel(labelText: any): Chainable<any>
    findCardTitle(title: string): Chainable<any>
    formField(label: string): Chainable<any>
    assignFieldValue(value: string): Chainable<any>
    fieldShouldHaveValue(value: string): Chainable<any>
    noteByTitle(noteTopology: string): Chainable<any>
    pageIsNotLoading(): Chainable<any>
    clearFocusedText(): Chainable<any>
    routerPush(fallback: any, name: any, params: any): Chainable<any>
    routerToRoot(): Chainable<any>
  }
}
