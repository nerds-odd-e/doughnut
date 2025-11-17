const submittableForm = {
  fill(noteAttributes: Record<string, string | undefined>) {
    for (const propName in noteAttributes) {
      const value = noteAttributes[propName]
      if (value) {
        cy.formField(propName).assignFieldValue(value)
      } else {
        cy.formField(propName).clear()
      }
    }
    return this
  },
  submit() {
    cy.get('input[value="Submit"]').click()
    cy.pageIsNotLoading()
  },
  submitWith(noteAttributes: Record<string, string | undefined>) {
    return this.fill(noteAttributes).submit()
  },
}

export default submittableForm
