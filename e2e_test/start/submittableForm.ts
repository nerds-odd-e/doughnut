const submittableForm = {
  submitWith: (noteAttributes: Record<string, string | undefined>) => {
    for (const propName in noteAttributes) {
      const value = noteAttributes[propName]
      if (value) {
        cy.formField(propName).assignFieldValue(value)
      } else {
        cy.formField(propName).clear()
      }
    }
    cy.get('input[value="Submit"]').click()
    cy.pageIsNotLoading()
  },
}

export default submittableForm
