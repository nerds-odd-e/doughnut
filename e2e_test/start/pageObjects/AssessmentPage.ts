
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
        getQuestionSection() {
          // TODO: check if this pattern is used
          return cy.get('[data-test="question-section"]')
        },
        getStemText() {
            return this.getQuestionSection().get('[data-test="stem"]').first().invoke("text")
        },
        answerFirst() {
          return this.getQuestionSection().get('button').first().click()
        },
        answerFromTable(answersTable: Record<string, string>[]) {
          return this.getStemText().then((stem) => {
            const row = answersTable.find(row => row.question === stem)
            cy.findByText(row.answer).click()
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
            return this.aQuestion().answerFromTable(answersTable).then(tryContinueAssessment)
          } else {
            return cy.log('Element not found')
          }
        })
      }

      return this.aQuestion().answerFromTable(answersTable).then(tryContinueAssessment)
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
