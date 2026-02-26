const getField = (label: string) => cy.findByLabelText(label)

const fieldFormControl = (field: string) =>
  getField(field).closest('.daisy-form-control')

const clickRadioByLabel = (labelText: string) => {
  cy.findByText(labelText, { selector: 'label' }).click({ force: true })
}

const assignFieldValue = (label: string, value: string) => {
  getField(label).then(($input) => {
    if ($input.attr('type') === 'file') {
      cy.fixture(value).then((img) => {
        cy.wrap($input).attachFile({
          fileContent: Cypress.Blob.base64StringToBlob(img),
          fileName: value,
          mimeType: 'image/png',
        })
      })
    } else if ($input.attr('role') === 'radiogroup') {
      clickRadioByLabel(value)
    } else if ($input.attr('role') === 'button') {
      cy.wrap($input).click()
      clickRadioByLabel(value)
    } else {
      cy.wrap($input).clear().type(value)
    }
  })
}

const fieldShouldHaveValue = (label: string, value: string) => {
  getField(label).should('have.value', value)
}

const form = {
  getField,
  assignFieldValue,
  fieldShouldHaveValue,
  clickRadioByLabel,
  expectFieldError(field: string, message: string) {
    fieldFormControl(field).find('.daisy-text-error').findByText(message)
  },
  expectNoFieldError(field: string) {
    fieldFormControl(field).find('.daisy-text-error').should('not.exist')
  },
  fill(noteAttributes: Record<string, string | undefined>) {
    for (const propName in noteAttributes) {
      const value = noteAttributes[propName]
      if (value) {
        getField(propName)
          .then(($input) => {
            const isFileInput = $input.attr('type') === 'file'
            const isSelect = $input.prop('tagName') === 'SELECT'
            return { $input, isFileInput, isSelect }
          })
          .then(({ $input, isFileInput, isSelect }) => {
            if (isSelect) {
              cy.wrap($input).select(value)
              fieldShouldHaveValue(propName, value)
            } else {
              assignFieldValue(propName, value)
              if (!isFileInput) {
                fieldShouldHaveValue(propName, value)
              }
            }
          })
      } else {
        getField(propName).clear()
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
