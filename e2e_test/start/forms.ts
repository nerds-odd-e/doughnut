const form = {
  fill(noteAttributes: Record<string, string | undefined>) {
    for (const propName in noteAttributes) {
      const value = noteAttributes[propName]
      if (value) {
        cy.formField(propName)
          .then(($input) => {
            const isFileInput = $input.attr('type') === 'file'
            return { $input, isFileInput }
          })
          .then(({ $input, isFileInput }) => {
            cy.wrap($input).assignFieldValue(value)
            if (!isFileInput) {
              cy.wrap($input).fieldShouldHaveValue(value)
            }
          })
      } else {
        cy.formField(propName).clear()
      }
    }
    return this
  },
}

const submittableForm = {
  fill(noteAttributes: Record<string, string | undefined>) {
    form.fill(noteAttributes)
    return submittableForm
  },
  submit() {
    cy.get('input[value="Submit"]').click()
    cy.pageIsNotLoading()
  },
  submitWith(noteAttributes: Record<string, string | undefined>) {
    return this.fill(noteAttributes).submit()
  },
}

export { form, submittableForm }
