import { questionPage } from './questionPage'

export const addQuestionPage = () => {
  cy.findByRole('button', { name: 'Add Question' }).click()
  return {
    ...questionPage,
  }
}
