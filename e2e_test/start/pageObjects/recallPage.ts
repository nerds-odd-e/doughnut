import { RecallsController } from '@generated/doughnut-backend-api/sdk.gen'
import { commonSenseSplit } from 'support/string_util'
import { waitUntilAppIsNotBusy } from '../pageBase'
import router from '../router'

function recallProgressFromTriple(triple: string) {
  const [finished, dailyTotal, totalAssimilated] = triple.split('/').map(Number)
  return {
    finished,
    toRepeatCount: (dailyTotal ?? 0) - (finished ?? 0),
    totalAssimilated,
  }
}

function loadRecallPage(options?: { waitForQuestion?: boolean }) {
  if (options?.waitForQuestion) {
    cy.intercept('GET', '**/api/memory-trackers/**/question**').as(
      'recallQuestion'
    )
  }
  cy.visit('/recall')
  if (options?.waitForQuestion) {
    cy.wait('@recallQuestion', { timeout: 15000 })
  }
  waitUntilAppIsNotBusy()
}

const recallPage = () => {
  return {
    yesIRemember() {
      cy.on('uncaught:exception', (err) => {
        if (
          err.message.includes('Unauthorized') ||
          err.message.includes('401')
        ) {
          return false
        }
        return true
      })
      cy.findByRole('button', { name: 'Yes, I remember' })
      cy.tick(11 * 1000).then(() => {
        cy.findByRole('button', { name: 'Yes, I remember' }).click({})
      })
    },
    typeSpellingAnswer(answer: string) {
      waitUntilAppIsNotBusy()
      cy.get('[data-test="question-section"]', { timeout: 15000 })
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
          this.yesIRemember()
        }
      })
    },
    expectCurrentQuestion() {
      // Verify we're back to the quiz view (current question) by checking that
      // the question section exists, which means we're viewing a question, not an answered question
      waitUntilAppIsNotBusy()
      cy.get('[data-test="question-section"]').should('exist')
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
        $el.findByText(`${numberOfNotes}`, { selector: '.recall-count' })
      })
      return this
    },
    visitRecallPage() {
      loadRecallPage()
      return recallPage()
    },
    visitRecallPageAndWaitForQuestion() {
      loadRecallPage({ waitForQuestion: true })
      return recallPage()
    },
    navigateToRecallPage() {
      router().toRoot()
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
