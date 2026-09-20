/**
 * Second-clone checkout: pull accepted history, then commit and publish from
 * the receiver. Shared, destination-addressed checkout plumbing lives in
 * `notebookCloneCheckoutDestination.ts`.
 */
import type { CliNotebookCheckoutState } from '../../../config/cliE2eNotebookCloneTasks'
import {
  commitNoteChangesAt,
  expectCanonicalTreeAt,
  expectCheckoutFileAt,
  expectCheckoutFileBytesAt,
  expectCheckoutFileExactTextAt,
  expectCleanAcceptedHeadAt,
  expectCleanNotebookAcceptedHeadAt,
  readCheckoutStateAt,
  runInstalledOn,
} from './notebookCloneCheckoutDestination'

function notebookCloneCheckoutReceiver() {
  return {
    /**
     * Commits a content-only edit in `@cliCloneReceiverDestination` after
     * that checkout has received accepted history. Sets `@cliNotebookPublishHead`.
     */
    commitReceiverEdit(
      relativePath: string,
      content: string
    ): Cypress.Chainable<null> {
      return commitNoteChangesAt('cliCloneReceiverDestination', [
        { relativePath, content },
      ])
    },
    publishReceiver(): Cypress.Chainable<null> {
      return runInstalledOn(
        'cliCloneReceiverDestination',
        'publish',
        'runInstalledCli'
      )
    },
    /**
     * Pulls accepted history onto `@cliCloneReceiverDestination` without
     * publishing. Captures the pre-pull head for the ancestor assertion and
     * the post-pull tip as `@cliNotebookRebasedCheckout` (same alias as the
     * primary-checkout pull path) for publish acceptance.
     */
    pullReceiver(): Cypress.Chainable<null> {
      return cy
        .get<string>('@cliCloneReceiverDestination')
        .then((checkoutDir) =>
          cy
            .task<CliNotebookCheckoutState>(
              'readCliNotebookCheckoutState',
              checkoutDir
            )
            .then((original) => {
              cy.wrap(original.head).as('cliCloneReceiverOriginalHead')
              return runInstalledOn(
                'cliCloneReceiverDestination',
                'pull',
                'runInstalledCli'
              ).then(() =>
                cy
                  .task<CliNotebookCheckoutState>(
                    'readCliNotebookCheckoutState',
                    checkoutDir
                  )
                  .then((rebased) => {
                    cy.wrap(rebased).as('cliNotebookRebasedCheckout')
                    return cy.wrap(null)
                  })
              )
            })
        )
    },
    expectReceiverCanonicalTreeFor(
      seededEntries: string[]
    ): Cypress.Chainable<null> {
      return expectCanonicalTreeAt('cliCloneReceiverDestination', seededEntries)
    },
    expectReceiverOriginalHeadIsAncestor(): Cypress.Chainable<null> {
      return cy
        .get<string>('@cliCloneReceiverDestination')
        .then((checkoutDir) =>
          cy.get<string>('@cliCloneReceiverOriginalHead').then((originalHead) =>
            cy
              .task<boolean>('cliNotebookCheckoutIsAncestorOfHead', {
                checkoutDir,
                ancestor: originalHead,
              })
              .then((isAncestor) => {
                expect(
                  isAncestor,
                  `original head ${originalHead} should be an ancestor of HEAD in ${checkoutDir}`
                ).to.equal(true)
                return cy.wrap(null)
              })
          )
        )
    },
    expectReceiverAtAcceptedHead(): Cypress.Chainable<null> {
      return expectCleanAcceptedHeadAt('cliCloneReceiverDestination')
    },
    expectReceiverAtNotebookAcceptedHead(
      notebookName: string
    ): Cypress.Chainable<null> {
      return expectCleanNotebookAcceptedHeadAt(
        'cliCloneReceiverDestination',
        notebookName
      )
    },
    /**
     * After a multi-commit publish, the receiver pull must keep the publisher's
     * original tip SHA and its first-parent chain (A → B → C), not a rewritten tip.
     */
    expectReceiverPreservesPublisherHistory(): Cypress.Chainable<null> {
      return readCheckoutStateAt('cliCloneDestination').then((publisher) =>
        readCheckoutStateAt('cliCloneReceiverDestination').then((receiver) => {
          expect(
            receiver.head,
            'receiver HEAD should equal publisher tip C'
          ).to.equal(publisher.head)
          expect(
            receiver.parent,
            'receiver parent should equal publisher B'
          ).to.equal(publisher.parent)
          expect(
            receiver.grandparent,
            'receiver grandparent should equal publisher A'
          ).to.equal(publisher.grandparent)
          expect(receiver.status, 'receiver checkout should be clean').to.equal(
            ''
          )
          expect(
            publisher.parent,
            'publisher should have intermediate commit B'
          ).to.not.equal('')
          expect(
            publisher.grandparent,
            'publisher should retain original A below B'
          ).to.not.equal('')
          expect(
            publisher.head,
            'publisher tip C should differ from B'
          ).to.not.equal(publisher.parent)
          expect(
            publisher.parent,
            'publisher B should differ from A'
          ).to.not.equal(publisher.grandparent)
          return cy.wrap(null)
        })
      )
    },
    expectReceiverCheckoutFile(
      relativePath: string,
      content: string
    ): Cypress.Chainable<null> {
      return expectCheckoutFileAt(
        'cliCloneReceiverDestination',
        relativePath,
        content
      )
    },
    expectReceiverCheckoutFileBytes(
      relativePath: string,
      spacedHex: string
    ): Cypress.Chainable<null> {
      return expectCheckoutFileBytesAt(
        'cliCloneReceiverDestination',
        relativePath,
        spacedHex
      )
    },
    expectReceiverCheckoutFileExactText(
      relativePath: string,
      text: string
    ): Cypress.Chainable<null> {
      return expectCheckoutFileExactTextAt(
        'cliCloneReceiverDestination',
        relativePath,
        text
      )
    },
  }
}

export { notebookCloneCheckoutReceiver }
