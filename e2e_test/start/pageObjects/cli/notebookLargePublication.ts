import type { Report } from '@generated/donut-backend-api'
import { NotebookController } from '@generated/donut-backend-api/sdk.gen'
import testability from '../../testability'
import { unwrapData } from '../../unwrapApi'
import { donutNotebookContent } from '../../donutNotebookContent'
import { notebookCloneCheckout } from './notebookCloneCheckout'

// The accepted Git listing must exceed one MiB, Node's default subprocess
// buffer. Long folder and title names reach that with few notes: their bare
// paths alone exceed it.
const noteCount = 2250
const batchSize = 250
const folderSegments = ['Large-notebook-shelf-', 'Large-notebook-drawer-'].map(
  (segment) => segment.repeat(9)
)
const folder = folderSegments.join('/')
const title = (index: number) =>
  `${'Accepted-note-'.repeat(10)}${String(index).padStart(5, '0')}`
const relativePath = (index: number) => `${folder}/${title(index)}.md`
const body = (index: number) => `Published small edit ${index}`

export const notebookLargePublication = {
  seed(notebookName: string) {
    const batches = Array.from({ length: noteCount / batchSize }, (_, batch) =>
      Array.from({ length: batchSize }, (_, offset) => {
        const index = batch * batchSize + offset
        return {
          Title: title(index),
          Folder: folder,
          Content: `---\ntype: Note\n---\nAccepted body ${index}\n`,
        }
      })
    )
    return cy
      .get<string>('@injectNotesExternalIdentifier')
      .then((owner) =>
        cy
          .wrap(batches, { log: false })
          .each((notes) =>
            testability().injectNotes(notes, owner, notebookName)
          )
      )
      .then(() =>
        testability().resnapshotNotebookGitBindingForTestability(notebookName)
      )
  },
  expectHealthyLargeAcceptedTree(notebookName: string) {
    return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
      cy
        .task<{ bytes: number; entries: number; head: string }>(
          'measureCliNotebookListing',
          checkoutDir
        )
        .then((listing) => {
          expect(
            listing.bytes,
            'complete accepted listing bytes'
          ).to.be.greaterThan(1024 * 1024)
          expect(
            listing.entries,
            'all accepted notes and Git metadata'
          ).to.be.greaterThan(noteCount)
          cy.wrap(listing).as('largeNotebookBaseline')
          return testability()
            .getNotebookIdByName(notebookName)
            .then((notebookId) =>
              cy
                .then({ timeout: 60_000 }, () =>
                  NotebookController.diagnoseNotebookGit({
                    path: { notebook: notebookId },
                  })
                )
                .then((response) => {
                  const report = unwrapData<Report>(response)
                  expect(report.acceptedHead).to.equal(listing.head)
                  expect(report.acceptedTree).to.equal(report.liveTree)
                  expect(report.differences).to.deep.equal([])
                })
            )
        })
    )
  },
  commitThreeEdits() {
    return notebookCloneCheckout()
      .commitRelatedNoteChanges(
        Array.from({ length: 3 }, (_, index) => ({
          relativePath: relativePath(index),
          content: `---\ntype: Note\n---\n${body(index)}`,
        }))
      )
      .then(() =>
        cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
          cy
            .task<{ parent: string }>(
              'readCliNotebookCheckoutState',
              checkoutDir
            )
            .then((pending) =>
              cy
                .get<{ head: string }>('@largeNotebookBaseline')
                .then((baseline) => {
                  expect(
                    pending.parent,
                    'pending commit starts at accepted head'
                  ).to.equal(baseline.head)
                })
            )
        )
      )
  },
  expectFreshReceiver(notebookName: string) {
    notebookCloneCheckout().expectFreshCloneAtAcceptedHead()
    cy.get<string>('@cliCloneFreshDestination').then((checkoutDir) =>
      cy
        .get<{ head: string; entries: number }>('@largeNotebookBaseline')
        .then((baseline) => {
          cy.task<boolean>('cliNotebookCheckoutIsAncestorOfHead', {
            checkoutDir,
            ancestor: baseline.head,
          }).should('equal', true)
          cy.task<{ entries: number }>('measureCliNotebookListing', checkoutDir)
            .its('entries')
            .should('equal', baseline.entries)
          cy.get<{ relativePath: string; content: string }[]>(
            '@cliNotebookProposalFiles'
          ).each((file) =>
            cy
              .readFile(`${checkoutDir}/${file.relativePath}`)
              .should('equal', file.content)
          )
        })
    )
    return cy.wrap([0, 1, 2]).each((index) =>
      testability()
        .getInjectedNoteIdByTitle(title(index))
        .then((noteId) => {
          donutNotebookContent.expectNoteByIdAt(
            noteId,
            [notebookName, ...folderSegments, title(index)],
            body(index)
          )
        })
    )
  },
}
