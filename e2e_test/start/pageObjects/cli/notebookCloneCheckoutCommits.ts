/**
 * Commits staged in the primary cloned checkout (`@cliCloneDestination`)
 * before a publish. Each method names the Portable-tree change a scenario
 * makes and records the resulting commit as `@cliNotebookPublishHead`.
 */
import { commitNoteChangesAt } from './notebookCloneCheckoutDestination'

type PrimaryCheckoutCommitTask =
  | 'commitCliNotebookCheckoutNoteRemoval'
  | 'commitCliNotebookCheckoutNoteRename'
  | 'commitCliNotebookCheckoutNoteRenameAndEdit'
  | 'commitCliNotebookCheckoutNoteRenameAndEmptyKeep'
  | 'commitCliNotebookCheckoutNoteRenameAndRemoval'
  | 'commitCliNotebookCheckoutTextAndBinaryFiles'
  | 'commitCliNotebookCheckoutFilledAttachment'
  | 'amendCliNotebookCheckoutExactBytes'

function commitNoteChanges(
  files: { relativePath: string; content: string }[]
): Cypress.Chainable<null> {
  return commitNoteChangesAt('cliCloneDestination', files)
}

function commitPrimaryCheckoutWith(
  task: PrimaryCheckoutCommitTask,
  change: Record<string, string | number>
): Cypress.Chainable<null> {
  return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
    cy.task<string>(task, { checkoutDir, ...change }).then((head) => {
      cy.wrap(head).as('cliNotebookPublishHead')
      return cy.wrap(null)
    })
  )
}

function notebookCloneCheckoutCommits() {
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
    /** One commit adding a UTF-8 text file and an exact-byte file. */
    commitTextAndBinaryFiles(
      textRelativePath: string,
      content: string,
      binaryRelativePath: string,
      bytes: string
    ): Cypress.Chainable<null> {
      return commitPrimaryCheckoutWith(
        'commitCliNotebookCheckoutTextAndBinaryFiles',
        {
          textRelativePath,
          content,
          binaryRelativePath,
          bytes,
        }
      )
    },
    /**
     * One unpublished commit adding a filled attachment of exact length; records
     * the proposal for retention checks after a rejected publish.
     * `fillByteHex` is a single byte such as `0x41`.
     */
    commitFilledAttachment(
      relativePath: string,
      byteLength: number,
      fillByteHex: string
    ): Cypress.Chainable<null> {
      const fillByte = Number.parseInt(fillByteHex.replace(/^0x/i, ''), 16)
      expect(
        fillByte,
        `fill byte ${fillByteHex} should be a single hex byte`
      ).to.be.within(0, 255)
      return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
        cy
          .task<string>('commitCliNotebookCheckoutFilledAttachment', {
            checkoutDir,
            relativePath,
            byteLength,
            fillByte,
          })
          .then((head) => {
            cy.wrap(head).as('cliNotebookPublishHead')
            cy.wrap([{ relativePath, byteLength, fillByte }]).as(
              'cliNotebookProposalFiles'
            )
            return cy.wrap(null)
          })
      )
    },
    /**
     * Amends the unpublished tip replacing one attachment with spaced-hex bytes.
     */
    amendExactBytes(
      relativePath: string,
      bytes: string
    ): Cypress.Chainable<null> {
      return commitPrimaryCheckoutWith('amendCliNotebookCheckoutExactBytes', {
        relativePath,
        bytes,
      })
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
  }
}

export { notebookCloneCheckoutCommits }
