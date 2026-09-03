import { RecallsController } from '@generated/donut-backend-api/sdk.gen'
import { commonSenseSplit } from 'support/string_util'
import { waitUntilAppIsNotBusy } from '../pageBase'
import router from '../router'
import { assumeQuestionPage } from './QuizQuestionPage'
import { recallDailyProbeMethods } from './recallDailyProbeMethods'
import { recallLearningSessionMethods } from './recallLearningSessionMethods'

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
    chooseGood() {
      cy.on('uncaught:exception', (err) => {
        if (
          err.message.includes('Unauthorized') ||
          err.message.includes('401')
        ) {
          return false
        }
        return true
      })
      cy.findByRole('button', { name: 'Good' })
      cy.tick(11 * 1000).then(() => {
        cy.findByRole('button', { name: 'Good' }).click({})
      })
      waitUntilAppIsNotBusy()
    },
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
    expectRecallProgressFromTriple(numberOfRecalls: string) {
      const { finished, toRepeatCount, totalAssimilated } =
        recallProgressFromTriple(numberOfRecalls)
      const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
      cy.wrap(
        RecallsController.recalling({ query: { timezone, dueindays: 0 } }),
        {
          log: false,
        }
      ).then((dueMemoryTrackers) => {
        return cy
          .wrap(RecallsController.previouslyAnswered({ query: { timezone } }), {
            log: false,
          })
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
    recallNotes(noteTitles: string) {
      waitUntilAppIsNotBusy()
      commonSenseSplit(noteTitles, ',').forEach((title) => {
        if (title === 'end') {
          cy.findByText(
            'You have finished all recalls for this half a day!'
          ).should('be.visible')
        } else {
          cy.findByText(title, { selector: 'h2 *' })
          this.chooseGood()
        }
      })
    },
    expectCurrentQuestion() {
      waitUntilAppIsNotBusy()
      assumeQuestionPage().getQuestionSection().should('be.visible')
      return this
    },
  }
}
export const recall = () => {
  const getRecallListItemInSidebar = (
    fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
  ) => cy.get('.main-menu').within(() => fn(cy.get('li[title="Recall"]')))

  return {
    expectCount(numberOfNotes: number) {
      getRecallListItemInSidebar(($el) => {
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
      cy.findByLabelText('Resume').should('exist')
      return this
    },
    resumeRecall() {
      cy.findByLabelText('Resume').click()
      waitUntilAppIsNotBusy()
      // Flush Vue remount timeouts under cy.clock() so the restored question is clickable.
      cy.tick(1)
      assumeQuestionPage().getQuestionSection().should('be.visible')
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
      getRecallListItemInSidebar(($el) => {
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
