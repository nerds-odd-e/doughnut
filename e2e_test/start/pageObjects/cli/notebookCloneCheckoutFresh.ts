/**
 * A checkout cloned after publication: it carries no earlier local history, so
 * what it holds came from the notebook's accepted head alone.
 */
import {
  expectCanonicalTreeAt,
  expectCheckoutFileBytesAt,
  expectCheckoutFileExactTextAt,
  expectCleanAcceptedHeadAt,
} from './notebookCloneCheckoutDestination'

function notebookCloneCheckoutFresh() {
  return {
    expectFreshCloneCanonicalTreeFor(
      seededEntries: string[]
    ): Cypress.Chainable<null> {
      return expectCanonicalTreeAt('cliCloneFreshDestination', seededEntries)
    },
    expectFreshCloneAtAcceptedHead(): Cypress.Chainable<null> {
      return expectCleanAcceptedHeadAt('cliCloneFreshDestination')
    },
    expectFreshCloneFileExactText(
      relativePath: string,
      text: string
    ): Cypress.Chainable<null> {
      return expectCheckoutFileExactTextAt(
        'cliCloneFreshDestination',
        relativePath,
        text
      )
    },
    expectFreshCloneFileBytes(
      relativePath: string,
      spacedHex: string
    ): Cypress.Chainable<null> {
      return expectCheckoutFileBytesAt(
        'cliCloneFreshDestination',
        relativePath,
        spacedHex
      )
    },
  }
}

export { notebookCloneCheckoutFresh }
