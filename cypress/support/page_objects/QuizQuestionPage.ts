
const questionWithStem = (stem: string) => {
  cy.findByText(stem)
}

export { questionWithStem }
