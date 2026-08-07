import { addQuestionPage } from './addQuestionPage'

export const questionListPage = () => {
  return {
    addQuestionPage() {
      cy.findByRole('button', { name: 'Add Question' }).click()
      return addQuestionPage()
    },
    expectQuestions(expectedQuestions: Record<string, string>[]) {
      expectedQuestions.forEach((row) => {
        cy.findByText(row.Question!).should(($el) => {
          expect(
            $el.text().trim(),
            `Expected question list to include ${JSON.stringify(row.Question)}`
          ).to.equal(row.Question!)
        })
        cy.findByText(row['Correct Choice']!).should(($el) => {
          expect(
            $el.hasClass('correct-choice'),
            `Expected correct choice ${JSON.stringify(row['Correct Choice'])} to be marked in the question list`
          ).to.equal(true)
        })
      })
      return this
    },
  }
}
