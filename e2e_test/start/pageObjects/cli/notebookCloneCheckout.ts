/**
 * CLI cloned-checkout page objects: commit, installed publish/pull, and
 * reading back Git parent/blob state for the rebase journey.
 */
import type { CliNotebookCheckoutState } from '../../../config/cliE2eNotebookCloneTasks'
import { notebookCloneCheckoutRebaseObservations } from './notebookCloneCheckoutRebase'
import { nonInteractiveOutput } from './outputAssertions'

function notebookCloneCheckout() {
  function commitNoteChanges(
    files: { relativePath: string; content: string }[]
  ): Cypress.Chainable<null> {
    return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
      cy
        .task<string>('commitCliNotebookCheckoutNoteChange', {
          checkoutDir,
          files,
        })
        .then((head) => {
          cy.wrap(head).as('cliNotebookPublishHead')
          cy.wrap(
            files.map(({ relativePath, content }) => ({
              relativePath,
              content: `${content}\n`,
            }))
          ).as('cliNotebookProposalFiles')
          return cy.wrap(null)
        })
    )
  }

  function readCheckoutState(): Cypress.Chainable<CliNotebookCheckoutState> {
    return cy
      .get<string>('@cliCloneDestination')
      .then((destination) =>
        cy.task<CliNotebookCheckoutState>(
          'readCliNotebookCheckoutState',
          destination
        )
      )
  }

  function runInstalledOnCheckout(
    subcommand: 'publish' | 'pull',
    task: 'runInstalledCli' | 'runInstalledCliExpectingRejection'
  ): Cypress.Chainable<null> {
    return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
      cy.get<string>('@donutPath').then((donutPath) =>
        cy.get<string>('@cliConfigDir').then((configDir) =>
          cy.task<null>(task, {
            donutPath,
            args: ['notebook', subcommand, checkoutDir],
            env: { DONUT_CONFIG_DIR: configDir },
          })
        )
      )
    )
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
      return commitNoteChanges(files)
    },
    commitRemoval(relativePath: string): Cypress.Chainable<null> {
      return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
        cy
          .task<string>('commitCliNotebookCheckoutNoteRemoval', {
            checkoutDir,
            relativePath,
          })
          .then((head) => {
            cy.wrap(head).as('cliNotebookPublishHead')
            return cy.wrap(null)
          })
      )
    },
    commitRename(
      fromRelativePath: string,
      toRelativePath: string
    ): Cypress.Chainable<null> {
      return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
        cy
          .task<string>('commitCliNotebookCheckoutNoteRename', {
            checkoutDir,
            fromRelativePath,
            toRelativePath,
          })
          .then((head) => {
            cy.wrap(head).as('cliNotebookPublishHead')
            return cy.wrap(null)
          })
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
      return cy.get<string>('@cliCloneDestination').then((destination) => {
        cy.get<string>('@cliNotebookPublishHead').then((head) => {
          readCheckoutState().then((state) => {
            expect(state.head).to.equal(head)
            expect(state.status).to.equal('')
          })
        })
        cy.get<{ relativePath: string; content: string }[]>(
          '@cliNotebookProposalFiles'
        ).each(({ relativePath, content }) => {
          cy.readFile(`${destination}/${relativePath}`).should('equal', content)
        })
        return cy.wrap(null)
      })
    },
    expectCommittedHeadAccepted(): Cypress.Chainable<null> {
      return cy
        .get<string>('@cliNotebookPublishHead')
        .then((head) => expectPublishedAcceptedHead(head))
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
      return cy.get<string>('@cliCloneDestination').then((destination) => {
        cy.readFile(`${destination}/${relativePath}`).should(
          'equal',
          `${content}\n`
        )
        return cy.wrap(null)
      })
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
      return cy.get<string>('@cliCloneDestination').then((destination) => {
        cy.task<string[]>('listNotebookCheckoutEntries', destination).should(
          'deep.equal',
          [...seededEntries].sort()
        )
        return cy.wrap(null)
      })
    },
    ...notebookCloneCheckoutRebaseObservations(),
  }
}

export { notebookCloneCheckout }
