const assumeQuestionPage = (stem?: string) => {
  if (stem) {
    cy.findByText(stem)
  }
  const question = () => (stem ? cy.findByText(stem).parent().parent() : cy)
  return {
    forNotebook(notebook: string) {
      cy.findByText(notebook, { selector: '.notebook-source *' })
    },
    isDisabled() {
      question().find('ol button').should('be.disabled')
    },

    isNotDisabled() {
      question().find('ol button').should('not.be.disabled')
    },
    skipQuestion() {
      cy.findByRole('button', { name: 'Move to end of list' }).click()
    },
  }
}

export { assumeQuestionPage }
