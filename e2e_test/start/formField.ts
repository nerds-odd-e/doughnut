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
          self.clickOption(value)
        } else if ($input.attr('role') === 'button') {
          cy.wrap($input).click()
          self.clickOption(value)
        } else {
          cy.wrap($input).clear().type(value)
        }
      })
      return self
    },
    clickOption(optionLabel: string) {
      cy.findByText(optionLabel, { selector: 'label' }).click({ force: true })
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

export { formField }
