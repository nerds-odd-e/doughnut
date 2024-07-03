import start from "../index";

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
    aQuestion() {
      return {
        getStem() {
            return cy.get(".quiz-instruction div").first().invoke("text")
        },
        answerFirst() {
          cy.get(".choices button").first().click()
        },
        answerFromTable(hashes: Record<string, string>[]) {
          this.getStem().then((stem) => {
            const row = hashes.find(row => row.question === stem)
            return cy.findByText(row.answer).click()
          })
        }
      }
    },
    expectEndOfAssessment(expectedScore: string) {
      cy.contains(expectedScore)
    },
  }
}

export const assumeAssessmentResultPage = () => {
  cy.findByRole("heading", { name: "Improve your knowledge by studying these notes" })

  return {
     expectCardFor(noteName: string) {
       cy.get('.card-body').should("contain", noteName)
     }
  }
}
