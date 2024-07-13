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
        const row = answersTable.find((row) => row.Question === stem)
        return this.answer(row!.Answer ?? '')
      })
    },
    answer(answer: string) {
      return cy.findByText(answer).click()
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
