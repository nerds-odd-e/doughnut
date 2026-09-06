/**
 * CLI notebook clone page objects. Domain: `donut notebook clone <notebook-id> <destination>`
 * (non-interactive, installed-binary invocation — same `runInstalledCli` mechanism as
 * `installation()`), and reading back the resulting Git checkout.
 */
import type { CliNotebookCheckoutState } from '../../../config/cliE2eNotebookCloneTasks'
import testability from '../../testability'
import { nonInteractiveOutput } from './outputAssertions'

function notebookClone() {
  function cloneWithTask(
    notebookName: string,
    task: 'runInstalledCli' | 'runInstalledCliExpectingRejection'
  ): Cypress.Chainable<null> {
    return testability()
      .getNotebookIdByName(notebookName)
      .then((notebookId) =>
        cy
          .task<string>('createCliNotebookCloneDestination')
          .then((destination) => {
            cy.wrap(destination).as('cliCloneDestination')
            return cy.get<string>('@donutPath').then((donutPath) =>
              cy.get<string>('@cliConfigDir').then((configDir) =>
                cy.get<string>('@savedAccessToken').then((token) =>
                  cy
                    .task<null>('writeCliAccessToken', {
                      configDir,
                      token,
                    })
                    .then(() =>
                      cy.task<null>(task, {
                        donutPath,
                        args: [
                          'notebook',
                          'clone',
                          String(notebookId),
                          destination,
                        ],
                        env: { DONUT_CONFIG_DIR: configDir },
                      })
                    )
                )
              )
            )
          })
      )
  }
  return {
    useAccessTokenOf(userIdentifier: string) {
      return cy
        .wrap(`access-token-of-${userIdentifier}`, { log: false })
        .as('savedAccessToken')
    },
    /**
     * Test-only setup helper: rebuilds the notebook's `NotebookGitBinding` after a fixture seeds
     * unsupported structural changes such as folders or readmes. It establishes the initial
     * accepted baseline; do not use it after the content edit under test.
     */
    resnapshotGitBinding(notebookName: string) {
      return testability().resnapshotNotebookGitBindingForTestability(
        notebookName
      )
    },

    /**
     * Resolves the notebook's numeric id, writes the owner's saved access token into the
     * isolated `@cliConfigDir`, then runs the installed CLI's non-interactive clone command
     * into a fresh test-owned destination (aliased `@cliCloneDestination`).
     */
    cloneNotebookInto(notebookName: string): Cypress.Chainable<null> {
      return cloneWithTask(notebookName, 'runInstalledCli')
    },
    cloneNotebookExpectingRejection(
      notebookName: string
    ): Cypress.Chainable<null> {
      return cloneWithTask(notebookName, 'runInstalledCliExpectingRejection')
    },
    expectDestinationAbsent(): Cypress.Chainable<null> {
      return cy.get<string>('@cliCloneDestination').then((destination) => {
        cy.task<boolean>(
          'cliNotebookCloneDestinationExists',
          destination
        ).should('equal', false)
        return cy.wrap(null)
      })
    },
  }
}

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

  function publishWithTask(
    task: 'runInstalledCli' | 'runInstalledCliExpectingRejection'
  ): Cypress.Chainable<null> {
    return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
      cy.get<string>('@donutPath').then((donutPath) =>
        cy.get<string>('@cliConfigDir').then((configDir) =>
          cy.task<null>(task, {
            donutPath,
            args: ['notebook', 'publish', checkoutDir],
            env: { DONUT_CONFIG_DIR: configDir },
          })
        )
      )
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
    publish(): Cypress.Chainable<null> {
      return publishWithTask('runInstalledCli')
    },
    publishExpectingRejection(): Cypress.Chainable<null> {
      return publishWithTask('runInstalledCliExpectingRejection')
    },
    expectProposalRetained(): Cypress.Chainable<null> {
      return cy.get<string>('@cliCloneDestination').then((destination) => {
        cy.get<string>('@cliNotebookPublishHead').then((head) => {
          cy.task<CliNotebookCheckoutState>(
            'readCliNotebookCheckoutState',
            destination
          ).then((state) => {
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
        .then((head) =>
          nonInteractiveOutput().expectContains(
            `Published notebook. Accepted head: ${head}`
          )
        )
    },
    /** Uses the system `git` executable (ADR 0002): one branch, one parentless commit, no dirt. */
    expectCleanSingleCommitCheckoutOnBranch(
      branch: string
    ): Cypress.Chainable<null> {
      return cy.get<string>('@cliCloneDestination').then((destination) => {
        cy.task<CliNotebookCheckoutState>(
          'readCliNotebookCheckoutState',
          destination
        ).then((state) => {
          expect(state.branch).to.equal(branch)
          expect(state.rootCommitCount).to.equal('1')
          expect(state.status).to.equal('')
        })
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
  }
}

export { notebookClone, notebookCloneCheckout }
