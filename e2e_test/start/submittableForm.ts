const submittableForm = {
  submitWith: (noteAttributes: Record<string, string>) => {
    for (const propName in noteAttributes) {
      const value = noteAttributes[propName]
      if (value) {
        cy.formField(propName).assignFieldValue(value)
      }
    }
    cy.get('input[value="Submit"]').click()
  },
}

export default submittableForm
