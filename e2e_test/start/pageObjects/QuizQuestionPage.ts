const assumeQuestionPage = (stem?: string) => {
  if (stem) {
    cy.findByText(stem)
  }
  const question = () => (stem ? cy.findByText(stem).parent().parent() : cy)
  const getQuestionSection = () => cy.get('[data-test="question-section"]')
  return {
    getQuestionSection,
    getStemText() {
      return getQuestionSection()
        .get('[data-test="stem"]')
        .first()
        .invoke('text')
    },
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
    answerFirstOption() {
      return getQuestionSection().find('button').first().click()
    },
    answer(answer: string) {
      getQuestionSection()
        .should('be.visible')
        .within(() => {
          cy.findByText(answer).should('be.visible').click()
        })
      cy.pageIsNotLoading()
      // Wait for the answered overlay to disappear, indicating it moved to the next stage
      // The gray overlay (daisy-bg-base-100/80) should disappear when the question moves to the next stage
      // This check is safe for both recall and assessment flows - if overlay doesn't exist, it passes immediately
      cy.get('.daisy-relative .daisy-absolute.daisy-bg-base-100\\/80', {
        timeout: 5000,
      }).should('not.exist')
      return this
    },
  }
}

export { assumeQuestionPage }
