import { addQuestionPage } from './addQuestionPage'
import { SuggestQuestionForFineTuningPage } from './SuggestQuestionForFineTuningPage'

export const questionListPage = () => {
  return {
    addQuestionPage: () => {
      cy.findByRole('button', { name: 'Add Question' }).click()
      return addQuestionPage()
    },
    expectQuestion(expectedQuestions: Record<string, string>[]) {
      expectedQuestions.forEach((row) => {
        cy.findByText(row.Question!)
        cy.findByText(row['Correct Choice']!).then(($el) => {
          cy.wrap($el).should('have.class', 'correct-choice')
        })
      })
    },
    suggestingQuestionForFineTuning(stem: string) {
      cy.findByText(stem).click()
      cy.findByRole('button', {
        name: 'Send this question for fine tuning the question generation model',
      }).click()
      return SuggestQuestionForFineTuningPage()
    },
  }
}
