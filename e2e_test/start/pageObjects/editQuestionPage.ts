import { questionPage } from './questionPage'

export const editQuestionPage = (questionNo: number) => {
  cy.findAllByRole('button', { name: 'Edit' })
    .eq(questionNo - 1)
    .click()
  return {
    ...questionPage,
  }
}
