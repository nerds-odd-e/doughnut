import { pageIsNotLoading } from '../pageBase'
import { form } from '../forms'

export const SuggestQuestionForFineTuningPage = () => {
  cy.contains(
    'will make this note and question visible to admin. Are you sure?'
  )
  return {
    confirm() {
      cy.findByRole('button', { name: 'OK' }).click()
      pageIsNotLoading()
    },
    comment(comment: string) {
      form.getField('Comment').type(comment)
      return this
    },
    suggestingPositiveFeedbackForFineTuning() {
      cy.findByRole('button', { name: '👍 Good' }).click()
      this.confirm()
    },
    suggestingNegativeFeedbackFineTuningExclusion() {
      cy.findByRole('button', { name: '👎 Bad' }).click()
      this.confirm()
    },
    submittingNoFeedback() {
      this.confirm()
    },
  }
}
