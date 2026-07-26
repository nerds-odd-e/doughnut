import { addQuestionPage } from './addQuestionPage'

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
    selectQuestion(questionStem: string) {
      cy.findByRole('checkbox', {
        name: `Select question ${questionStem}`,
      }).check()
      return this
    },
    deleteSelectedQuestions() {
      cy.intercept(
        'DELETE',
        '**/api/predefined-questions/**/note-questions/delete'
      ).as('deleteQuestions')
      cy.findByRole('button', { name: 'Delete Question' }).click()
      cy.findByRole('button', { name: 'Confirm' }).click()
      cy.wait('@deleteQuestions').then(({ response }) => {
        expect(response?.statusCode, 'delete questions').to.equal(200)
      })
      return this
    },
    expectDeleteSuccessToast() {
      cy.contains('.Vue-Toastification__toast--success', 'Delete success', {
        timeout: 8000,
      }).should('be.visible')
      return this
    },
    expectQuestionAbsent(questionStem: string) {
      cy.contains('.question-table', questionStem).should('not.exist')
      cy.findByText(questionStem).should('not.exist')
      return this
    },
  }
}
