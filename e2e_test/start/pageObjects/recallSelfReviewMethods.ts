import { commonSenseSplit } from 'support/string_util'
import { waitUntilAppIsNotBusy } from '../pageBase'
import { assumeQuestionPage } from './QuizQuestionPage'

export const recallSelfReviewMethods = () => {
  return {
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
    selfReviewNotes(noteTitles: string) {
      waitUntilAppIsNotBusy()
      commonSenseSplit(noteTitles, ',').forEach((title) => {
        if (title === 'end') {
          cy.findByText(
            'You have finished all recalls for this half a day!'
          ).should('be.visible')
        } else {
          this.chooseSelfReview().expectRecalling(title)
          this.chooseGood()
        }
      })
    },
    chooseSelfReview() {
      waitUntilAppIsNotBusy()
      cy.findByText("We couldn't load this question.").should('be.visible')
      cy.findByRole('button', { name: 'Self-review' }).click()
      waitUntilAppIsNotBusy()
      assumeQuestionPage().getQuestionSection().should('be.visible')
      return this
    },
    expectRecalling(noteTitle: string) {
      cy.findByText(noteTitle, { selector: 'h2 *' })
      return this
    },
  }
}
