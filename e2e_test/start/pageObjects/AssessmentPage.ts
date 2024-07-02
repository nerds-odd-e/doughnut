export const assumeAssessmentPage = (notebook?: string) => {
  if (notebook) {
    cy.findByRole("heading", { name: `Assessment For ${notebook}` })
  }

  return {
    expectQuestion(stem: string) {
      cy.findByText(stem)
      return {
        answer(answer: string) {
          cy.findByText(answer).click()
        },
      }
    },
    expectAQuestion() {
      return {
        getStem() {
            return cy.get(".quiz-instruction div").first().invoke("text")
        },
        answerFirst() {
          cy.get(".choices button").first().click()
        },
      }
    },
    expectEndOfAssessment(expectedScore: string) {
      cy.contains(expectedScore)
    }
  }
}
