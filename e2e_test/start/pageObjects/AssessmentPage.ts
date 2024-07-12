const assumeQuestionSection = () => {
  return {
    getQuestionSection() {
      return cy.get('[data-test="question-section"]')
    },
    getStemText() {
      return this.getQuestionSection()
        .get('[data-test="stem"]')
        .first()
        .invoke('text')
    },
    answerFirstOption() {
      return this.getQuestionSection().get('button').first().click()
    },
    answerFromTable(answersTable: Record<string, string>[]) {
      return this.getStemText().then((stem) => {
        const row = answersTable.find((row) => row.question === stem)
        return this.answer(row!.answer ?? '')
      })
    },
    answer(answer: string) {
      // eslint-disable-next-line cypress/no-unnecessary-waiting
      return cy.findByText(answer).click().wait(100)
      // This wait is an anti-pattern and should be fixed.
      // There is an issue now when the user clicks an answer,
      // the test move on before the next question appears on the screen.
      // We should have something else in the questions page like a status bar so we can use  cy.get() and .should() instead.
    },
  }
}

export const assumeAssessmentPage = (notebook?: string) => {
  if (notebook) {
    cy.findByRole('heading', { name: `Assessment For ${notebook}` })
  }

  return {
    expectQuestion(stem: string) {
      cy.findByText(stem)
      return assumeQuestionSection()
    },
    assumeQuestionSection,
    expectEndOfAssessment(expectedScore: string) {
      cy.contains(expectedScore)
    },
    answerQuestionsFromTable(answersTable: Record<string, string>[]) {
      const tryContinueAssessment = () => {
        return cy.get('body').then(($body) => {
          if ($body.find('.quiz-instruction').length > 0) {
            return this.assumeQuestionSection()
              .answerFromTable(answersTable)
              .then(tryContinueAssessment)
          } else {
            return cy.log('No more questions to answer.')
          }
        })
      }

      return this.assumeQuestionSection()
        .answerFromTable(answersTable)
        .then(tryContinueAssessment)
    },
  }
}
