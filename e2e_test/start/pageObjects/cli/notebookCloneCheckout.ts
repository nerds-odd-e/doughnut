/**
 * The primary cloned checkout (`@cliCloneDestination`): installed publish and
 * pull, and the accepted-head claims a scenario makes about them. Assembles
 * the commit, fresh-clone, observation, rebase and receiver page objects into
 * the single `cli.notebookCloneCheckout()` surface used by the steps.
 */
import type { CliNotebookCheckoutState } from '../../../config/cliE2eNotebookCloneTasks'
import { notebookCloneCheckoutCommits } from './notebookCloneCheckoutCommits'
import {
  expectCheckoutFileAt,
  expectCleanAcceptedHeadAt,
  readCheckoutStateAt,
  runInstalledOn,
} from './notebookCloneCheckoutDestination'
import { notebookCloneCheckoutFresh } from './notebookCloneCheckoutFresh'
import { notebookCloneCheckoutObservations } from './notebookCloneCheckoutObservations'
import { notebookCloneCheckoutRebaseObservations } from './notebookCloneCheckoutRebase'
import { notebookCloneCheckoutReceiver } from './notebookCloneCheckoutReceiver'
import { nonInteractiveOutput } from './outputAssertions'

function notebookCloneCheckout() {
  function readCheckoutState(): Cypress.Chainable<CliNotebookCheckoutState> {
    return readCheckoutStateAt('cliCloneDestination')
  }

  function runInstalledOnCheckout(
    subcommand: 'publish' | 'pull',
    task: 'runInstalledCli' | 'runInstalledCliExpectingRejection'
  ): Cypress.Chainable<null> {
    return runInstalledOn('cliCloneDestination', subcommand, task)
  }

  function expectPublishedAcceptedHead(head: string): Cypress.Chainable<null> {
    return nonInteractiveOutput().expectContains(
      `Published notebook. Accepted head: ${head}`
    )
  }

  return {
    publish(): Cypress.Chainable<null> {
      return runInstalledOnCheckout('publish', 'runInstalledCli')
    },
    publishExpectingRejection(): Cypress.Chainable<null> {
      return runInstalledOnCheckout(
        'publish',
        'runInstalledCliExpectingRejection'
      )
    },
    /**
     * Captures original L as `@cliNotebookOriginalCheckout`, then runs installed
     * `notebook pull`. Observed L′ is `@cliNotebookRebasedCheckout`;
     * `@cliNotebookPublishHead` remains L.
     */
    pull(): Cypress.Chainable<null> {
      return readCheckoutState().then((original) => {
        cy.wrap(original).as('cliNotebookOriginalCheckout')
        return runInstalledOnCheckout('pull', 'runInstalledCli').then(() =>
          readCheckoutState().then((rebased) => {
            cy.wrap(rebased).as('cliNotebookRebasedCheckout')
            return cy.wrap(null)
          })
        )
      })
    },
    /**
     * Captures original L as `@cliNotebookOriginalCheckout`, then runs installed
     * `notebook pull` expecting rejection. Does not capture L′; native continue
     * records `@cliNotebookResolvedCheckout`.
     */
    pullExpectingRejection(): Cypress.Chainable<null> {
      return readCheckoutState().then((original) => {
        cy.wrap(original).as('cliNotebookOriginalCheckout')
        return runInstalledOnCheckout(
          'pull',
          'runInstalledCliExpectingRejection'
        )
      })
    },
    continueWithChosenEdit(
      relativePath: string,
      content: string
    ): Cypress.Chainable<null> {
      return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
        cy
          .task<null>('continueCliNotebookCheckoutRebaseWithChosenBytes', {
            checkoutDir,
            relativePath,
            content,
          })
          .then(() =>
            readCheckoutState().then((resolved) => {
              cy.wrap(resolved).as('cliNotebookResolvedCheckout')
              return cy.wrap(null)
            })
          )
      )
    },
    expectProposalRetained(): Cypress.Chainable<null> {
      expectCleanAcceptedHeadAt('cliCloneDestination')
      cy.get<{ relativePath: string; content: string }[]>(
        '@cliNotebookProposalFiles'
      ).each(({ relativePath, content }) => {
        expectCheckoutFileAt('cliCloneDestination', relativePath, content)
      })
      return cy.wrap(null)
    },
    expectCommittedHeadAccepted(): Cypress.Chainable<null> {
      return cy
        .get<string>('@cliNotebookPublishHead')
        .then((head) => expectPublishedAcceptedHead(head))
    },
    expectPublishedHeadIsSingleCommitAfterReceiverOriginal(): Cypress.Chainable<null> {
      return readCheckoutState().then((publisher) =>
        cy
          .get<string>('@cliCloneReceiverOriginalHead')
          .then((receiverOriginalHead) => {
            expect(
              publisher.parent,
              'published checkout should contain exactly one commit after the receiver original head'
            ).to.equal(receiverOriginalHead)
            return cy.wrap(null)
          })
      )
    },
    expectRebasedHeadAccepted(): Cypress.Chainable<null> {
      return cy
        .get<CliNotebookCheckoutState>('@cliNotebookRebasedCheckout')
        .then((rebased) => expectPublishedAcceptedHead(rebased.head))
    },
    expectResolvedHeadAccepted(): Cypress.Chainable<null> {
      return cy
        .get<CliNotebookCheckoutState>('@cliNotebookResolvedCheckout')
        .then((resolved) => expectPublishedAcceptedHead(resolved.head))
    },
    expectCheckoutFile(
      relativePath: string,
      content: string
    ): Cypress.Chainable<null> {
      return expectCheckoutFileAt('cliCloneDestination', relativePath, content)
    },
    ...notebookCloneCheckoutCommits(),
    ...notebookCloneCheckoutFresh(),
    ...notebookCloneCheckoutObservations(),
    ...notebookCloneCheckoutReceiver(),
    ...notebookCloneCheckoutRebaseObservations(),
  }
}

export { notebookCloneCheckout }
