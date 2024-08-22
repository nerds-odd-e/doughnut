export default () => ({
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
