/**
 * CLI cloned-checkout page objects: commit, installed publish/pull, and
 * reading back Git parent/blob state for the rebase journey.
 */
import type { CliNotebookCheckoutState } from '../../../config/cliE2eNotebookCloneTasks'
import { notebookCloneCheckoutObservations } from './notebookCloneCheckoutObservations'
import { notebookCloneCheckoutRebaseObservations } from './notebookCloneCheckoutRebase'
import {
  commitNoteChangesAt,
  expectCheckoutFileAt,
  expectCleanAcceptedHeadAt,
  notebookCloneCheckoutReceiver,
  readCheckoutStateAt,
  runInstalledOn,
} from './notebookCloneCheckoutReceiver'
import { nonInteractiveOutput } from './outputAssertions'

type PrimaryCheckoutCommitTask =
  | 'commitCliNotebookCheckoutNoteRemoval'
  | 'commitCliNotebookCheckoutNoteRename'
  | 'commitCliNotebookCheckoutNoteRenameAndEdit'
  | 'commitCliNotebookCheckoutNoteRenameAndEmptyKeep'
  | 'commitCliNotebookCheckoutNoteRenameAndRemoval'

function notebookCloneCheckout() {
  function commitNoteChanges(
    files: { relativePath: string; content: string }[]
  ): Cypress.Chainable<null> {
    return commitNoteChangesAt('cliCloneDestination', files)
  }

  function readCheckoutState(): Cypress.Chainable<CliNotebookCheckoutState> {
    return readCheckoutStateAt('cliCloneDestination')
  }

  function commitPrimaryCheckoutWith(
    task: PrimaryCheckoutCommitTask,
    change: Record<string, string>
  ): Cypress.Chainable<null> {
    return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
      cy.task<string>(task, { checkoutDir, ...change }).then((head) => {
        cy.wrap(head).as('cliNotebookPublishHead')
        return cy.wrap(null)
      })
    )
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
    commitEdit(relativePath: string, content: string): Cypress.Chainable<null> {
      return commitNoteChanges([{ relativePath, content }])
    },
    commitAddition(
      relativePath: string,
      content: string
    ): Cypress.Chainable<null> {
      return commitNoteChanges([{ relativePath, content }])
    },
    commitRelatedNoteChanges(
      files: { relativePath: string; content: string }[]
    ): Cypress.Chainable<null> {
      return commitNoteChanges(
        files.map(({ relativePath, content }) => ({
          relativePath,
          content: content.replace(/\\n/g, '\n'),
        }))
      )
    },
    commitRemoval(relativePath: string): Cypress.Chainable<null> {
      return commitPrimaryCheckoutWith('commitCliNotebookCheckoutNoteRemoval', {
        relativePath,
      })
    },
    commitRename(
      fromRelativePath: string,
      toRelativePath: string
    ): Cypress.Chainable<null> {
      return commitPrimaryCheckoutWith('commitCliNotebookCheckoutNoteRename', {
        fromRelativePath,
        toRelativePath,
      })
    },
    commitRenameAndRemoval(
      fromRelativePath: string,
      toRelativePath: string,
      removeRelativePath: string
    ): Cypress.Chainable<null> {
      return commitPrimaryCheckoutWith(
        'commitCliNotebookCheckoutNoteRenameAndRemoval',
        {
          fromRelativePath,
          toRelativePath,
          removeRelativePath,
        }
      )
    },
    commitRenameAndEmptyKeep(
      fromRelativePath: string,
      toRelativePath: string,
      keepRelativePath: string
    ): Cypress.Chainable<null> {
      return commitPrimaryCheckoutWith(
        'commitCliNotebookCheckoutNoteRenameAndEmptyKeep',
        {
          fromRelativePath,
          toRelativePath,
          keepRelativePath,
        }
      )
    },
    commitRenameAndEdit(
      fromRelativePath: string,
      toRelativePath: string,
      relativePath: string,
      content: string
    ): Cypress.Chainable<null> {
      return commitPrimaryCheckoutWith(
        'commitCliNotebookCheckoutNoteRenameAndEdit',
        { fromRelativePath, toRelativePath, relativePath, content }
      )
    },
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
    ...notebookCloneCheckoutObservations(),
    ...notebookCloneCheckoutReceiver(),
    ...notebookCloneCheckoutRebaseObservations(),
  }
}

export { notebookCloneCheckout }
