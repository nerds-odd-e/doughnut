import { addDays, formatDateToISO } from "../../support/dateUtils"

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
    answerWrongFromTable(answersTable: Record<string, string>[]) {
      return this.getStemText().then((stem) => {
        const row = answersTable.find((row) => row.Question === stem)
        return this.answer(row!['One Wrong Choice'] ?? '')
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
      for (let i = 0; i < answersTable.length; i++) {
        this.assumeQuestionSection().answerFromTable(answersTable)
      }
    },
    answerQuestionsByScore(score: number) {
      const answersTable: Record<string, string>[] = [
        {
          Question: 'Where in the world is Singapore?',
          Answer: 'Asia',
          'One Wrong Choice': 'europe',
        },
        {
          Question: 'Most famous food of Vietnam?',
          Answer: 'Pho',
          'One Wrong Choice': 'bread',
        },
      ]
      const expectCorrectAssessmentCount = score / (100 / answersTable.length)
      let correctAnswer = 0
      for (let i = 0; i < answersTable.length; i++) {
        if (correctAnswer >= expectCorrectAssessmentCount) {
          this.assumeQuestionSection().answerWrongFromTable(answersTable)
        } else {
          this.assumeQuestionSection().answerFromTable(answersTable)
          correctAnswer += 1
        }
      }
    },
    getCertificate(notebook: string, certifiedBy: string) {
      cy.findByRole('button', { name: 'Get Certificate' }).click()
      cy.contains('This to certificate that')
      cy.contains('Old Learner')
      cy.findByText(certifiedBy)
      cy.contains(notebook)
    },
    getExpiredDate(expiredDays: number) {
      cy.findByRole('button', { name: 'Get Certificate' }).click()

      const now = new Date()
      const expDate = addDays(now, expiredDays)
      const expiredDate = formatDateToISO(expDate)

      cy.get('[data-cy="expired-date"]').contains(expiredDate)
    },
    expectNotPassAssessment() {
      cy.findByRole('button', { name: 'Get Certificate' }).should('be.disabled')
    },
  }
}
