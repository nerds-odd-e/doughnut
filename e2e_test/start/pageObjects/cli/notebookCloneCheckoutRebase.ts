/**
 * L vs L′ observations after installed `notebook pull` on a cloned checkout.
 */
import type {
  CliNotebookCheckoutConflictState,
  CliNotebookCheckoutState,
} from '../../../config/cliE2eNotebookCloneTasks'
import { nonInteractiveOutput } from './outputAssertions'

function originalAndChildCheckout(childAlias: string): Cypress.Chainable<{
  original: CliNotebookCheckoutState
  child: CliNotebookCheckoutState
}> {
  return cy
    .get<CliNotebookCheckoutState>('@cliNotebookOriginalCheckout')
    .then((original) =>
      cy
        .get<CliNotebookCheckoutState>(childAlias)
        .then((child) => ({ original, child }))
    )
}

function originalAndRebasedCheckout(): Cypress.Chainable<{
  original: CliNotebookCheckoutState
  rebased: CliNotebookCheckoutState
}> {
  return originalAndChildCheckout('@cliNotebookRebasedCheckout').then(
    ({ original, child }) => ({ original, rebased: child })
  )
}

function originalAndResolvedCheckout(): Cypress.Chainable<{
  original: CliNotebookCheckoutState
  resolved: CliNotebookCheckoutState
}> {
  return originalAndChildCheckout('@cliNotebookResolvedCheckout').then(
    ({ original, child }) => ({ original, resolved: child })
  )
}

function expectCleanChildOfNewAcceptedHead(
  original: CliNotebookCheckoutState,
  child: CliNotebookCheckoutState,
  childLabel: string
): void {
  expect(child.status, `${childLabel} checkout should be clean`).to.equal('')
  expect(
    child.head,
    `${childLabel} should differ from original L`
  ).to.not.equal(original.head)
  expect(child.parent, `${childLabel} parent B should exist`).to.not.equal('')
  expect(
    child.head,
    `${childLabel} should be a child of B, not B itself`
  ).to.not.equal(child.parent)
  expect(
    child.parent,
    `${childLabel} parent B should differ from L’s parent`
  ).to.not.equal(original.parent)
}

function notebookCloneCheckoutRebaseObservations() {
  return {
    expectPausedRebaseConflictFor(
      relativePath: string
    ): Cypress.Chainable<null> {
      return cy.get<string>('@cliCloneDestination').then((destination) =>
        cy
          .task<CliNotebookCheckoutConflictState>(
            'readCliNotebookCheckoutConflictState',
            destination
          )
          .then((paused) => {
            expect(
              paused.rebaseMerge,
              'paused pull should leave rebase-merge'
            ).to.equal(true)
            expect(
              paused.unmerged,
              `paused pull should leave unmerged ${relativePath}`
            ).to.include(relativePath)
            return cy.wrap(null)
          })
      )
    },
    expectCleanRebasedChildOfAcceptedHead(): Cypress.Chainable<null> {
      return originalAndRebasedCheckout().then(({ original, rebased }) => {
        expectCleanChildOfNewAcceptedHead(original, rebased, 'L′')
        return nonInteractiveOutput().expectContains(
          `Unpublished local commit: ${rebased.head}. Accepted head: ${rebased.parent}.`
        )
      })
    },
    /**
     * Graph of resolved L′ after native continue. Does not require L′ bytes to
     * match original L (same-note conflict resolution may change them).
     */
    expectCleanResolvedChildOfAcceptedHead(): Cypress.Chainable<null> {
      return originalAndResolvedCheckout().then(({ original, resolved }) => {
        expectCleanChildOfNewAcceptedHead(original, resolved, 'resolved L′')
        return cy.wrap(null)
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
    expectResolvedLocalCommitFor(
      relativePath: string
    ): Cypress.Chainable<null> {
      return originalAndResolvedCheckout().then(({ original, resolved }) => {
        expect(
          resolved.author,
          `resolved commit author for ${relativePath}`
        ).to.equal(original.author)
        expect(
          resolved.message,
          `resolved commit message for ${relativePath}`
        ).to.equal(original.message)
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
