const clickRadioByLabel = (labelText: string) => {
  cy.findByText(labelText, { selector: 'label' }).click({ force: true })
}

const formControl = (label: string) =>
  cy.findByLabelText(label).closest('.daisy-form-control')

const inputElement = (label: string) => cy.findByLabelText(label)

const formField = (label: string) => {
  const self = {
    assignValue(value: string) {
      inputElement(label).then(($input) => {
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
      return self
    },
    shouldHaveValue(value: string) {
      inputElement(label).should('have.value', value)
      return self
    },
    expectError(message: string) {
      formControl(label).find('.daisy-text-error').findByText(message)
      return self
    },
    expectNoError() {
      formControl(label).find('.daisy-text-error').should('not.exist')
      return self
    },
    type(text: string) {
      inputElement(label).type(text)
      return self
    },
    clear() {
      inputElement(label).clear()
      return self
    },
    check() {
      inputElement(label).check()
      return self
    },
    click() {
      inputElement(label).click()
      return self
    },
    within(callback: () => void) {
      inputElement(label).within(callback)
      return self
    },
    shouldBeDisabled() {
      inputElement(label).should('be.disabled')
      return self
    },
    shouldNotBeDisabled() {
      inputElement(label).should('not.be.disabled')
      return self
    },
  }
  return self
}

const form = {
  getField: formField,
  assignFieldValue(label: string, value: string) {
    formField(label).assignValue(value)
  },
  fieldShouldHaveValue(label: string, value: string) {
    formField(label).shouldHaveValue(value)
  },
  clickRadioByLabel,
  expectFieldError(field: string, message: string) {
    formField(field).expectError(message)
  },
  expectNoFieldError(field: string) {
    formField(field).expectNoError()
  },
  fill(noteAttributes: Record<string, string | undefined>) {
    for (const propName in noteAttributes) {
      const value = noteAttributes[propName]
      if (value) {
        inputElement(propName)
          .then(($input) => {
            const isFileInput = $input.attr('type') === 'file'
            const isSelect = $input.prop('tagName') === 'SELECT'
            return { $input, isFileInput, isSelect }
          })
          .then(({ $input, isFileInput, isSelect }) => {
            if (isSelect) {
              cy.wrap($input).select(value)
              formField(propName).shouldHaveValue(value)
            } else {
              formField(propName).assignValue(value)
              if (!isFileInput) {
                formField(propName).shouldHaveValue(value)
              }
            }
          })
      } else {
        formField(propName).clear()
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
