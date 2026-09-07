/**
 * L vs L′ observations after installed `notebook pull` on a cloned checkout.
 */
import type { CliNotebookCheckoutState } from '../../../config/cliE2eNotebookCloneTasks'
import { nonInteractiveOutput } from './outputAssertions'

function originalAndRebasedCheckout(): Cypress.Chainable<{
  original: CliNotebookCheckoutState
  rebased: CliNotebookCheckoutState
}> {
  return cy
    .get<CliNotebookCheckoutState>('@cliNotebookOriginalCheckout')
    .then((original) =>
      cy
        .get<CliNotebookCheckoutState>('@cliNotebookRebasedCheckout')
        .then((rebased) => ({ original, rebased }))
    )
}

function notebookCloneCheckoutRebaseObservations() {
  return {
    expectCleanRebasedChildOfAcceptedHead(): Cypress.Chainable<null> {
      return originalAndRebasedCheckout().then(({ original, rebased }) => {
        expect(rebased.status, 'rebased checkout should be clean').to.equal('')
        expect(rebased.head, 'L′ should differ from original L').to.not.equal(
          original.head
        )
        expect(rebased.parent, 'L′ parent B should exist').to.not.equal('')
        expect(
          rebased.head,
          'L′ should be a child of B, not B itself'
        ).to.not.equal(rebased.parent)
        expect(
          rebased.parent,
          'L′ parent B should differ from L’s parent'
        ).to.not.equal(original.parent)
        return nonInteractiveOutput().expectContains(
          `Unpublished local commit: ${rebased.head}. Accepted head: ${rebased.parent}.`
        )
      })
    },
    expectOriginalLocalCommitRetainedFor(
      relativePath: string
    ): Cypress.Chainable<null> {
      return originalAndRebasedCheckout().then(({ original, rebased }) => {
        expect(rebased.author).to.equal(original.author)
        expect(rebased.message).to.equal(original.message)
        expect(
          rebased.blobs[relativePath],
          `blob for ${relativePath} should retain L’s patch`
        ).to.equal(original.blobs[relativePath])
        return cy.wrap(null)
      })
    },
    expectCheckoutFileMatchesAcceptedParent(
      relativePath: string
    ): Cypress.Chainable<null> {
      return cy
        .get<CliNotebookCheckoutState>('@cliNotebookRebasedCheckout')
        .then((rebased) => {
          expect(
            rebased.blobs[relativePath],
            `${relativePath} at L′ should match accepted parent B`
          ).to.equal(rebased.parentBlobs[relativePath])
          return cy.wrap(null)
        })
    },
  }
}

export { notebookCloneCheckoutRebaseObservations }
