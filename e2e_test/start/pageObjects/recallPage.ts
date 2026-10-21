import { RecallsController } from '@generated/donut-backend-api/sdk.gen'
import {
  pageBar,
  waitUntilAppIsNotBusy,
  withinDonutMenuItem,
} from '../pageBase'
import router from '../router'
import { assumeQuestionPage } from './QuizQuestionPage'
import { recallDailyProbeMethods } from './recallDailyProbeMethods'
import { recallLearningSessionMethods } from './recallLearningSessionMethods'
import { recallSelfReviewMethods } from './recallSelfReviewMethods'

function recallProgressFromTriple(triple: string) {
  const [finished, dailyTotal, totalAssimilated] = triple.split('/').map(Number)
  return {
    finished,
    toRepeatCount: (dailyTotal ?? 0) - (finished ?? 0),
    totalAssimilated,
  }
}

function loadRecallPage(options?: { waitForQuestionCount?: number }) {
  const waitForQuestionCount = options?.waitForQuestionCount
  const shouldWaitForPrompts =
    waitForQuestionCount !== undefined && waitForQuestionCount > 0
  if (shouldWaitForPrompts) {
    cy.intercept(
      'GET',
      /\/api\/memory-trackers\/[^/]+\/recall-prompt(?:\?.*)?$/
    ).as('recallPrompt')
  }
  router().visitNamed('recall')
  if (shouldWaitForPrompts) {
    for (let i = 0; i < waitForQuestionCount; i++) {
      cy.wait('@recallPrompt', { timeout: 15000 })
    }
  }
  waitUntilAppIsNotBusy()
}

const recallPage = () => {
  return {
    ...recallLearningSessionMethods(),
    ...recallDailyProbeMethods(),
    ...recallSelfReviewMethods(),
    typeSpellingAnswer(answer: string) {
      waitUntilAppIsNotBusy()
      cy.get('[data-test="question-section"]:visible', { timeout: 15000 })
        .should('be.visible')
        .as('spellingQuestion')
      cy.get('@spellingQuestion')
        .find('input[placeholder="put your answer here"]')
        .should('be.visible')
        .clear()
        .invoke('val', answer)
        .trigger('input')
      cy.get('@spellingQuestion')
        .find('input[type="submit"][value="Answer"]')
        .click()
      waitUntilAppIsNotBusy()
    },
    expectOverlapRetryForSpelling(stem: string) {
      waitUntilAppIsNotBusy()
      assumeQuestionPage(stem)
      cy.findByTestId('spelling-overlap-feedback')
        .should('be.visible')
        .and(
          'contain.text',
          "Your answer matches an overlapped note, but that's different from the expected answer"
        )
      cy.get('[data-test="question-section"]:visible')
        .find('input[placeholder="put your answer here"]')
        .should('have.value', '')
        .and('be.focused')
      return this
    },
    expectRecallProgressFromTriple(numberOfRecalls: string) {
      const { finished, toRepeatCount, totalAssimilated } =
        recallProgressFromTriple(numberOfRecalls)
      const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
      cy.then(() =>
        RecallsController.recalling({ query: { timezone, dueindays: 0 } })
      ).then((dueMemoryTrackers) => {
        return cy
          .then(() =>
            RecallsController.previouslyAnswered({ query: { timezone } })
          )
          .then((previouslyAnswered) => {
            expect(
              previouslyAnswered?.length ?? 0,
              `recall finished today for ${numberOfRecalls}`
            ).to.eq(finished)
            expect(
              dueMemoryTrackers?.toRepeat?.length ?? 0,
              `recall queue length for ${numberOfRecalls}`
            ).to.eq(toRepeatCount)
            expect(
              dueMemoryTrackers?.totalAssimilatedCount,
              `total assimilated memory trackers for ${numberOfRecalls}`
            ).to.eq(totalAssimilated)
          })
      })
    },
    expectToRecallCounts(numberOfRecalls: string) {
      const { finished, toRepeatCount, totalAssimilated } =
        recallProgressFromTriple(numberOfRecalls)
      const dailyTotal = finished + toRepeatCount

      cy.get('.progress-bar').should(
        'contain',
        `Recalling: ${finished}/${dailyTotal}`
      )
      // Click progress bar to show recall session options dialog
      cy.get('.progress-bar').first().click()

      // Check dialog content
      cy.contains('Recall Session Options').should('be.visible')
      cy.get('.modal-body').within(() => {
        cy.contains(`Daily Progress: ${finished} / ${dailyTotal}`)
        cy.contains(`Total assimilated: ${finished} / ${totalAssimilated}`)
      })

      // Close dialog
      cy.get('.close-button').click()
    },
    repeatMore() {
      cy.findByRole('button', { name: 'Load more from next 3 days' }).click()
    },
    rememberCurrentQuestion() {
      waitUntilAppIsNotBusy()
      assumeQuestionPage()
        .getQuestionSection()
        .should('be.visible')
        .invoke('text')
        .then((text) => {
          expect(text.trim(), 'current recall question text').not.to.equal('')
          cy.wrap(text.trim()).as('currentRecallQuestion')
        })
      cy.get('.progress-bar').first().invoke('text').as('currentRecallProgress')
      return this
    },
    expectCurrentQuestion() {
      waitUntilAppIsNotBusy()
      assumeQuestionPage().getQuestionSection().should('be.visible')
      return this
    },
    expectRememberedQuestion() {
      waitUntilAppIsNotBusy()
      cy.get<string>('@currentRecallQuestion').then((question) => {
        assumeQuestionPage()
          .getQuestionSection()
          .should('be.visible')
          .invoke('text')
          .should((text) => {
            expect(text.trim(), 'preserved current recall question').to.equal(
              question
            )
          })
      })
      cy.get<string>('@currentRecallProgress').then((progress) => {
        cy.get('.progress-bar').first().should('have.text', progress)
      })
      return this
    },
  }
}
export const recall = () => {
  const getRecallInDonutMenu = (
    fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
  ) => withinDonutMenuItem('Recall', fn)

  return {
    expectCount(numberOfNotes: number) {
      getRecallInDonutMenu(($el) => {
        if (numberOfNotes === 0) {
          $el.get('.recall-count').should('not.exist')
        } else {
          $el.findByText(`${numberOfNotes}`, { selector: '.recall-count' })
        }
      })
      return this
    },
    expectPotentialLearningSession(count: number, notebookTitle: string) {
      this.visitRecallPage().expectPotentialLearningSession(
        count,
        notebookTitle
      )
      return this
    },
    expectResumeAvailable() {
      pageBar().findByRole('button', { name: 'Resume' }).should('be.visible')
      return this
    },
    resumeRecall() {
      pageBar().findByRole('button', { name: 'Resume' }).click()
      waitUntilAppIsNotBusy()
      // Flush Vue remount timeouts under cy.clock() so the restored question is clickable.
      cy.tick(1)
      cy.get('.progress-bar').first().should('be.visible')
      return recallPage()
    },
    visitRecallPage() {
      loadRecallPage()
      return recallPage()
    },
    visitRecallPageAndWaitForQuestions(count: number) {
      loadRecallPage({ waitForQuestionCount: count })
      return recallPage()
    },
    navigateToRecallPage() {
      router().push('root')
      getRecallInDonutMenu(($el) => {
        $el.click()
      })
      waitUntilAppIsNotBusy()
      return recallPage()
    },
    assumeRecallPage() {
      return recallPage()
    },
    expectRecallProgressFromTriple(numberOfRecalls: string) {
      recallPage().expectRecallProgressFromTriple(numberOfRecalls)
      return this
    },
  }
}
