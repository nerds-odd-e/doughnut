import { CertificatePopup } from './CertificatePopup'

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
        if (row.AnswerCorrect === 'true') {
          return this.answerCorrect(row!.Answer)
        } else {
          return this.answerIncorrect(row!.Answer)
        }
      })
    },
    answerCorrect(answer: string) {
      return cy.findByText(answer).click().pageIsNotLoading()
    },
    answerIncorrect(answer: string) {
      cy.findByText(answer).click()
      return cy.findByText('Continue').click().pageIsNotLoading()
    },
    answerWithoutContinuing(answer: string) {
      return cy.findByText(answer).click().pageIsNotLoading()
    },
  }
}

const endOfAssessment = () => {
  const findCertificateButton = () =>
    cy.findByRole('button', { name: 'View Certificate' })

  return {
    passAssessment() {
      cy.findByText('You have passed the assessment.').should('be.visible')
    },
    expectNotPassAssessment() {
      cy.findByText('You have not passed the assessment.').should('be.visible')
    },
    expectCertificate() {
      findCertificateButton().click()
      return CertificatePopup()
    },
    expectNoCertificate() {
      findCertificateButton().should('not.exist')
    },
    expectCertificateCannotBeObtained() {
      findCertificateButton().should('have.class', 'disabled')
    },
  }
}

export const assumeAssessmentPage = (notebook?: string) => {
  if (notebook) {
    cy.findByRole('heading', { name: `Assessment For ${notebook}` })
  }

  return {
    assumeQuestionSection,
    answerQuestionsFromTable(answersTable: Record<string, string>[]) {
      Cypress._.times(answersTable.length, () => {
        this.assumeQuestionSection().answerFromTable(answersTable)
      })
      cy.pageIsNotLoading()
    },
    answerYesNoQuestionsToScore(correctAnswers: number, allQuestions: number) {
      for (let i = 0; i < correctAnswers; i++) {
        this.assumeQuestionSection().answerCorrect('Yes')
        cy.pageIsNotLoading()
      }
      for (let i = correctAnswers; i < allQuestions; i++) {
        this.assumeQuestionSection().answerIncorrect('No')
        cy.pageIsNotLoading()
      }
    },
    expectEndOfAssessment(expectedScore?: string) {
      if (expectedScore) {
        cy.contains(expectedScore)
      }
      return endOfAssessment()
    },
  }
}
