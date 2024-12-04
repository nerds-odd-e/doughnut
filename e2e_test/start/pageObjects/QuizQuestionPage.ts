const assumeQuestionPage = (stem?: string) => {
  if (stem) {
    cy.findByText(stem)
  }
  const question = () => (stem ? cy.findByText(stem).parent().parent() : cy)
  const getChoice = (choice: string) =>
    question().findAllByRole('button', { name: choice })
  return {
    isDisabled() {
      question().find('ol button').should('be.disabled')
    },

    isNotDisabled() {
      question().find('ol button').should('not.be.disabled')
    },

    expectChoiceToBe(choice: string, correctness: 'correct' | 'incorrect') {
      getChoice(choice).click()
      getChoice(choice)
        .invoke('attr', 'class')
        .should('contain', `is-${correctness}`)
    },
  }
}

export { assumeQuestionPage }
