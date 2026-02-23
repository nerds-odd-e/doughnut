const form = {
  expectFieldError(field: string, message: string) {
    cy.formField(field)
      .closest('.daisy-form-control')
      .find('.daisy-text-error')
      .findByText(message)
  },
  fill(noteAttributes: Record<string, string | undefined>) {
    for (const propName in noteAttributes) {
      const value = noteAttributes[propName]
      if (value) {
        cy.formField(propName)
          .then(($input) => {
            const isFileInput = $input.attr('type') === 'file'
            const isSelect = $input.prop('tagName') === 'SELECT'
            return { $input, isFileInput, isSelect }
          })
          .then(({ $input, isFileInput, isSelect }) => {
            if (isSelect) {
              cy.wrap($input).select(value)
              cy.wrap($input).fieldShouldHaveValue(value)
            } else {
              cy.wrap($input).assignFieldValue(value)
              if (!isFileInput) {
                cy.wrap($input).fieldShouldHaveValue(value)
              }
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
    cy.get('body').then(($body) => {
      if ($body.find('input[value="Submit"]').length > 0) {
        cy.get('input[value="Submit"]').click()
      } else {
        cy.findByRole('button', { name: 'Save' }).click()
      }
      cy.pageIsNotLoading()
    })
  },
  submitWith(noteAttributes: Record<string, string | undefined>) {
    return this.fill(noteAttributes).submit()
  },
}

export { form, submittableForm }
