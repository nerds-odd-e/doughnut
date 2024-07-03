
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
            return cy.get(".quiz-instruction div", {}).first().invoke("text")
        },
        answerFirst() {
          cy.get(".choices button").first().click()
        },
        answerFromTable(answersTable: Record<string, string>[]) {
          return this.getStem().then((stem) => {
            const row = answersTable.find(row => row.question === stem)
            return cy.findByText(row.answer).click()
          })
        }
      }
    },
    expectEndOfAssessment(expectedScore: string) {
      cy.contains(expectedScore)
    },
    answerQuestionsFromTable(answersTable: Record<string, string>[]) {
      const tryContinueAssessment = () => {
        return cy.get('body').then($body => {
          if ($body.find('.quiz-instruction').length > 0) {
            return passNextQuestion();
          } else {
            return cy.log('Element not found');
          }
        });
      }

      const passNextQuestion = () => this.aQuestion().answerFromTable(answersTable).then(tryContinueAssessment)

      return passNextQuestion()
    }
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
