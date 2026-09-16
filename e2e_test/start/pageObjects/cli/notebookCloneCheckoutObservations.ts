/**
 * Observable tree and history state for the primary cloned checkout.
 */
import type { CliNotebookCheckoutState } from '../../../config/cliE2eNotebookCloneTasks'
import {
  expectCanonicalTreeAt,
  readCheckoutStateAt,
} from './notebookCloneCheckoutReceiver'
import { nonInteractiveOutput } from './outputAssertions'

function readCheckoutState(): Cypress.Chainable<CliNotebookCheckoutState> {
  return readCheckoutStateAt('cliCloneDestination')
}

function expectAncestorOfCheckoutHead(
  checkoutDir: string,
  ancestor: string,
  description: string
): Cypress.Chainable<null> {
  return cy
    .task<boolean>('cliNotebookCheckoutIsAncestorOfHead', {
      checkoutDir,
      ancestor,
    })
    .then((isAncestor) => {
      expect(isAncestor, description).to.equal(true)
      return cy.wrap(null)
    })
}

function notebookCloneCheckoutObservations() {
  return {
    expectCheckoutFileUnchangedFromParent(
      relativePath: string
    ): Cypress.Chainable<null> {
      return readCheckoutState().then((state) => {
        expect(
          state.blobs[relativePath],
          `${relativePath} should retain its accepted-parent blob`
        ).to.equal(state.parentBlobs[relativePath])
        return cy.wrap(null)
      })
    },
    expectCleanAppendOnlyChainFromOriginalHead(): Cypress.Chainable<null> {
      return cy
        .get<CliNotebookCheckoutState>('@cliNotebookOriginalCheckout')
        .then((original) =>
          cy
            .get<CliNotebookCheckoutState>('@cliNotebookRebasedCheckout')
            .then((pulled) => {
              expect(original.status, 'checkout at A should be clean').to.equal(
                ''
              )
              expect(pulled.status, 'checkout at C should be clean').to.equal(
                ''
              )
              expect(pulled.head, 'C should differ from B').to.not.equal(
                pulled.parent
              )
              expect(pulled.parent, 'B should differ from A').to.not.equal(
                original.head
              )
              expect(
                pulled.grandparent,
                'accepted history should be A → B → C'
              ).to.equal(original.head)
              return cy.wrap(null)
            })
        )
    },
    expectOriginalHeadIsAncestorAndCleanAcceptedHead(): Cypress.Chainable<null> {
      return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
        cy
          .get<CliNotebookCheckoutState>('@cliNotebookOriginalCheckout')
          .then((original) => {
            expect(original.status, 'checkout at A should be clean').to.equal(
              ''
            )
            return expectAncestorOfCheckoutHead(
              checkoutDir,
              original.head,
              `original head ${original.head} should be an ancestor of HEAD in ${checkoutDir}`
            ).then(() =>
              cy
                .get<CliNotebookCheckoutState>('@cliNotebookRebasedCheckout')
                .then((pulled) => {
                  expect(
                    pulled.status,
                    'received checkout should be clean'
                  ).to.equal('')
                  return nonInteractiveOutput().expectContains(
                    `Accepted head: ${pulled.head}`
                  )
                })
            )
          })
      )
    },
    recordAcceptedHead(notebookName: string): Cypress.Chainable<null> {
      return cy
        .task<string>('readNotebookAcceptedGitObjectId', notebookName)
        .then((head) =>
          cy.then(function (this: { webAcceptedHeads?: string[] }) {
            const previous = this.webAcceptedHeads ?? []
            expect(
              previous,
              `accepted head ${head} should be a new web-accepted commit`
            ).to.not.include(head)
            cy.wrap([...previous, head]).as('webAcceptedHeads')
            return cy.wrap(null)
          })
        )
    },
    expectRecordedAcceptedHeadsAreAncestorsAndCleanAcceptedHead(): Cypress.Chainable<null> {
      return cy
        .get<string>('@cliCloneDestination')
        .then((checkoutDir) =>
          cy
            .get<string[]>('@webAcceptedHeads')
            .then((heads) =>
              heads
                .reduce(
                  (chain, ancestor) =>
                    chain.then(() =>
                      expectAncestorOfCheckoutHead(
                        checkoutDir,
                        ancestor,
                        `web-accepted head ${ancestor} should be an ancestor of HEAD in ${checkoutDir}`
                      )
                    ),
                  cy.wrap(null) as Cypress.Chainable<null>
                )
                .then(() =>
                  this.expectOriginalHeadIsAncestorAndCleanAcceptedHead()
                )
            )
        )
    },
    expectCheckoutParentFile(
      relativePath: string,
      content: string
    ): Cypress.Chainable<null> {
      return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
        cy
          .task<string>('readCliNotebookCheckoutParentFile', {
            checkoutDir,
            relativePath,
          })
          .then((actual) => {
            expect(
              actual,
              `${relativePath} should be readable at accepted parent B`
            ).to.equal(content.trimEnd())
            return cy.wrap(null)
          })
      )
    },
    /** Uses the system `git` executable (ADR 0002): one branch, one parentless commit, no dirt. */
    expectCleanSingleCommitCheckoutOnBranch(
      branch: string
    ): Cypress.Chainable<null> {
      return readCheckoutState().then((state) => {
        expect(state.branch).to.equal(branch)
        expect(state.rootCommitCount).to.equal('1')
        expect(state.status).to.equal('')
        return cy.wrap(null)
      })
    },
    /** Canonical ADR-0004 tree only: seeded readmes/notes, no `.donut`, manifest, id, sidecar, or db files. */
    expectCanonicalTreeFor(seededEntries: string[]): Cypress.Chainable<null> {
      return expectCanonicalTreeAt('cliCloneDestination', seededEntries)
    },
  }
}

export { notebookCloneCheckoutObservations }
