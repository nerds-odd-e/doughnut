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
      cy.pageIsNotLoading()
      question()
        .find('ol button')
        .should(($buttons) => {
          expect(
            $buttons
              .toArray()
              .some(
                (btn) =>
                  Cypress.$(btn).hasClass('is-disabled') ||
                  Cypress.$(btn).hasClass('disabled') ||
                  Cypress.$(btn).hasClass('daisy-opacity-65')
              )
          ).to.be.true
        })
    },

    isNotDisabled() {
      cy.pageIsNotLoading()
      question()
        .find('ol button')
        .should(($buttons) => {
          expect(
            $buttons
              .toArray()
              .every(
                (btn) =>
                  !(
                    Cypress.$(btn).hasClass('is-disabled') ||
                    Cypress.$(btn).hasClass('disabled') ||
                    Cypress.$(btn).hasClass('daisy-opacity-65')
                  )
              )
          ).to.be.true
        })
    },
    skipQuestion() {
      cy.pageIsNotLoading()
      cy.findByRole('button', { name: 'Move to end of list' }).click()
    },
  }
}

export { assumeQuestionPage }
