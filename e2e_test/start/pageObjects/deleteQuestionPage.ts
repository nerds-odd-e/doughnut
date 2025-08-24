export const deleteQuestionPage = () => {
  return {
    deleteQuestions(questions: string[]) {
      questions.forEach((q) => {
        cy.findByLabelText(q).check()
      });

      cy.findByRole('button', { name: 'Submit' }).click()
    }
  }
}
