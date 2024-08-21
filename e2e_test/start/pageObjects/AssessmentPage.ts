import { assumeBazaarPage } from './bazaarPage'

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
      Cypress._.times(answersTable.length, () => {
        this.assumeQuestionSection().answerFromTable(answersTable)
      })
      cy.pageIsNotLoading()
    },
    answerYesNoQuestionsToScore(correctAnswers: number, allQuestions: number) {
      for (let i = 0; i < correctAnswers; i++) {
        this.assumeQuestionSection().answer('Yes')
        cy.pageIsNotLoading()
      }
      for (let i = correctAnswers; i < allQuestions; i++) {
        this.assumeQuestionSection().answer('No')
        cy.pageIsNotLoading()
      }
    },
    passAssessment() {
      cy.findByText('You have passed the assessment.').should('be.visible')
    },
    expectNotPassAssessment() {
      cy.findByText('You have not passed the assessment.').should('be.visible')
    },
    viewCertificate() {
      cy.findByText('View Certificate').click()
      cy.findByText(`is granted the Certified`).should('be.visible')
      cy.findByText(`${notebook}`).should('be.visible')
    },
    expectNoCertificate() {
      cy.findByText('View Certificate').should('not.exist')
    },
    viewCertificateWithDate(date: string) {
      this.viewCertificate()
      cy.findByText(date).should('be.visible')
    },
    expectCerticateHasExprityDate() {
      const nextYear: Date = new Date()
      nextYear.setFullYear(nextYear.getFullYear() + 1)

      cy.findByText('View Certificate').click()
      cy.findByTestId('expired-date').contains(
        nextYear.toISOString().split('T')[0]!
      )
    },
    expectReachedLimit() {
      cy.findByText(
        'You have reached the assessment limit for today. Please try again tomorrow.'
      )
      cy.findByRole('button', { name: 'OK' }).click()
      assumeBazaarPage()
    },
  }
}
