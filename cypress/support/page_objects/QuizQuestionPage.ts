
const questionWithStem = (stem: string) => {
  const stemElm = cy.findByText(stem)

  return {
    isDisabled() {
      stemElm.siblings("ol").find("button").should("be.disabled")
    }
  }
}

export { questionWithStem }
