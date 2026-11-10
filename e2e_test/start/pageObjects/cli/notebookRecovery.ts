import type { MemoryTracker, Report } from '@generated/donut-backend-api'
import { NotebookController } from '@generated/donut-backend-api/sdk.gen'
import testability from '../../testability'
import { unwrapData } from '../../unwrapApi'
import { readCheckoutStateAt } from './notebookCloneCheckoutDestination'
import { runInstalledOn } from './notebookCloneCheckoutExecution'
import type { CliNotebookCheckoutState } from '../../../config/cliE2eNotebookCloneTasks'
import { nonInteractiveOutput } from './outputAssertions'

function diagnosis(notebookName: string) {
  return testability()
    .getNotebookIdByName(notebookName)
    .then((notebookId) =>
      cy
        .then(() =>
          NotebookController.diagnoseNotebookGit({
            path: { notebook: notebookId },
          })
        )
        .then((response) => unwrapData<Report>(response))
    )
}

export const notebookRecovery = {
  rememberLearning(noteTitle: string) {
    return testability()
      .assimilateNote(noteTitle)
      .then(() =>
        testability().memoryTrackerForNote(noteTitle, 'UNDERSTANDING')
      )
      .as('recoveryLearningBefore')
  },
  injectLiveOnlyNote(notebookName: string, noteTitle: string, content: string) {
    return cy
      .get<string>('@injectNotesExternalIdentifier')
      .then((owner) =>
        testability().injectNoteWithContent(
          noteTitle,
          content,
          owner,
          notebookName
        )
      )
  },
  reviewLiveOnlyNote(notebookName: string, noteTitle: string) {
    return diagnosis(notebookName).then((report) => {
      expect(
        report.differences,
        'one authored live-only difference'
      ).to.have.length(1)
      expect(report.differences![0]!.path).to.equal(`${noteTitle}.md`)
      expect(report.differences![0]!.acceptedBlobId).to.equal(null)
      cy.wrap(report).as('recoveryReview')
      return readCheckoutStateAt('cliCloneDestination').then((pending) => {
        cy.wrap(pending).as('recoveryPendingBefore')
        return runInstalledOn(
          'cliCloneDestination',
          'diagnose',
          'runInstalledCli'
        ).then(() => {
          nonInteractiveOutput().expectContains(
            `Accepted head: ${report.acceptedHead}`
          )
          nonInteractiveOutput().expectContains(`Live tree: ${report.liveTree}`)
          return nonInteractiveOutput().expectContains(
            `live-only: ${noteTitle}.md`
          )
        })
      })
    })
  },
  recoverReviewedLiveEdits(notebookName: string) {
    return cy.get<Report>('@recoveryReview').then((review) =>
      runInstalledOn('cliCloneDestination', 'recover', 'runInstalledCli', [
        review.acceptedHead!,
        review.liveTree!,
      ]).then(() =>
        diagnosis(notebookName).then((after) => {
          expect(
            after.acceptedTree,
            'accepted tree is the reviewed live tree'
          ).to.equal(review.liveTree)
          expect(after.liveTree).to.equal(review.liveTree)
          expect(after.differences).to.deep.equal([])
          nonInteractiveOutput().expectContains(
            `Recovered notebook. Accepted head: ${after.acceptedHead}.`
          )
          return readCheckoutStateAt('cliCloneDestination').then(
            (afterCheckout) =>
              cy
                .get<CliNotebookCheckoutState>('@recoveryPendingBefore')
                .then((before) => {
                  expect(
                    afterCheckout,
                    'recovery retains the pending checkout'
                  ).to.deep.equal(before)
                })
          )
        })
      )
    )
  },
  expectLearningRetained(noteTitle: string) {
    return cy.get<MemoryTracker>('@recoveryLearningBefore').then((before) =>
      testability()
        .memoryTrackerForNote(noteTitle, 'UNDERSTANDING')
        .then((after) => {
          expect(after.id, 'same learning identity').to.equal(before.id)
          expect(after.note.id, 'same learned Note identity').to.equal(
            before.note.id
          )
          expect(after.recallCount).to.equal(before.recallCount)
          expect(after.nextRecallAt).to.equal(before.nextRecallAt)
          expect(after.lastRecalledAt).to.equal(before.lastRecalledAt)
        })
    )
  },
}
