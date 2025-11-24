export const assumeAssociateWikidataDialog = () => {
  cy.findByText('Associate Wikidata').should('be.visible')

  const withinModalContainer = (callback: () => void) => {
    cy.findByText('Associate Wikidata')
      .closest('.modal-container')
      .within(callback)
  }

  return {
    // Actions - Input/Selection
    associate(wikiID: string) {
      withinModalContainer(() => {
        cy.formField('Wikidata Id').assignFieldValue(wikiID).type('{enter}')
      })
      return this
    },
    setWikidataId(wikidataId: string) {
      withinModalContainer(() => {
        cy.formField('Wikidata Id').assignFieldValue(wikidataId)
      })
      return this
    },
    selectResult(wikidataID: string) {
      withinModalContainer(() => {
        cy.get(`[data-wikidata-id="${wikidataID}"]`).click()
      })
      return this
    },
    confirmAssociationWithDifferentLabel(wikidataTitle: string) {
      withinModalContainer(() => {
        // Wait for the title options to appear and check that the wikidata title is visible
        cy.findByText(/Suggested Title:/)
          .should('be.visible')
          .should('contain.text', wikidataTitle)
        // Select "Replace title" option - this will immediately save and close the dialog
        cy.findByText('Replace title').click()
      })
      // Dialog should close automatically after selecting Replace title
      return this
    },
    close() {
      withinModalContainer(() => {
        cy.findByRole('button', { name: 'Close' }).click()
      })
    },

    // Assertions
    expectErrorOnWikidataId(message: string) {
      withinModalContainer(() => {
        cy.expectFieldErrorMessage('Wikidata Id', message)
      })
      this.close()
      return this
    },
    expectWikidataIdValue(value: string) {
      withinModalContainer(() => {
        cy.formField('Wikidata Id').fieldShouldHaveValue(value)
        cy.findByRole('button', { name: 'Close' }).click()
      })
      return this
    },
    expectOpenLinkButtonToOpenUrl(url: string) {
      withinModalContainer(() => {
        const elm = cy.findByRole('button', { name: 'open link' })
        // Wait for the button to be visible (it appears when Wikidata ID is present)
        elm.should('be.visible')

        cy.window().then((win) => {
          const popupWindowStub = {
            location: { href: undefined },
            focus: cy.stub(),
          }
          cy.stub(win, 'open').as('open').returns(popupWindowStub)
          elm.click()
          cy.get('@open').should('have.been.calledWith', '')
          // using a callback so that cypress can wait until the stubbed value is assigned
          cy.wrap(() => popupWindowStub.location.href)
            .should((cb) => expect(cb()).equal(url))
            .then(() => {
              expect(popupWindowStub.focus).to.have.been.called
            })
        })
      })
      return this
    },
  }
}
