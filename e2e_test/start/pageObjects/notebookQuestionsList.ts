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
  expectNoQuestionsForTopic: (topicName: string) => {
    cy.get('.notebook-questions-list')
      .findByText(topicName)
      .parent()
      .findByText('No questions')
      .should('exist')
  },
  expectOnlyQuestionsForTopic: (topicName: string, question: string) => {
    cy.get('.notebook-questions-list')
      .findByText(topicName)
      .parent()
      .findByText(question)
      .should('exist')
  },
})
