import { waitUntilAppIsNotBusy } from '../pageBase'

const questionFormFields = [
  'Stem',
  'Choice 0',
  'Choice 1',
  'Choice 2',
  'Correct Choice Index',
] as const

export const addQuestionPage = () => {
  return {
    fillQuestion(row: Record<string, string>) {
      cy.findByRole('button', { name: '+' }).click()
      questionFormFields.forEach((key) => {
        if (row[key] !== undefined && row[key] !== '') {
          cy.findByLabelText(key)
            .clear()
            .invoke('val', row[key]!)
            .trigger('input')
        }
      })
      return this
    },
    addQuestion(row: Record<string, string>) {
      cy.intercept('POST', /\/api\/mcqs\/\d+$/).as('add')
      this.fillQuestion(row)
      cy.findByRole('button', { name: 'Submit' }).click()
      waitUntilAppIsNotBusy()
      cy.wait('@add').then(({ response }) => {
        expect(response?.statusCode, 'add').to.equal(200)
      })
      cy.get('.question-table').should('contain.text', row.Stem!)
      return this
    },
    generateQuestionWithAI() {
      cy.findByRole('button', { name: 'Generate by AI' }).click()
      waitUntilAppIsNotBusy()
      return this
    },
    refineQuestion(row: Record<string, string>) {
      this.fillQuestion(row)
      cy.findByRole('button', { name: 'Refine' }).click()
      waitUntilAppIsNotBusy()
      return this
    },
    expectQuestionInForm(expected: Record<string, string>) {
      questionFormFields.forEach((key) => {
        cy.findByLabelText(key).should(($input) => {
          const actual = String($input.val() ?? '')
          expect(
            actual,
            `Expected question form "${key}" to be ${JSON.stringify(expected[key])}, but found ${JSON.stringify(actual)}`
          ).to.equal(expected[key]!)
        })
      })
      return this
    },
  }
}
