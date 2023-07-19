const questionWithStem = (stem: string) => {
  const findStem = () => cy.findByText(stem)
  findStem()

  return {
    isDisabled() {
      findStem().siblings("ol").find("button").should("be.disabled")
    },
  }
}

export { questionWithStem }
