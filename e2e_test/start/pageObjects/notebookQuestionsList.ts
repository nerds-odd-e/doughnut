import { addQuestionPage } from './addQuestionPage'

export default () => ({
  addQuestionPage: (topicName: string) => {
    cy.get('.notebook-questions-list')
      .findByText(topicName)
      .parent()
      .findByRole('button', { name: 'Add Question' })
      .click()
    return addQuestionPage()
  },
  expectNoQuestionsForNote: (topicName: string) => {
    cy.get('.notebook-questions-list')
      .findByText(topicName)
      .parent()
      .findByText('No questions')
      .should('exist')
  },
  expectOnlyQuestionsForNote: (topicName: string, question: string) => {
    cy.get('.notebook-questions-list')
      .findByText(topicName)
      .parent()
      .findByText(question)
      .should('exist')
  },
  deleteQuestion: (topicName: string, question: string) => {
    cy.get('.notebook-questions-list')
      .findByText(topicName)
      .parent()
      .findByText(question)
      .parent()
      .findByRole('button', { name: 'Delete Question' })
      .click()
  },
})
