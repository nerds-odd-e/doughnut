import { waitUntilAppIsNotBusy } from '../pageBase'

const conversionFailure =
  'Could not turn your speech into text. Your recording is kept.'

const audioToolsPanel = () => cy.findByRole('region', { name: 'Audio tools' })

const audioToolsPage = () => {
  return {
    startRecording() {
      audioToolsPanel().within(() => {
        cy.findByRole('button', { name: 'Record' }).click()
        cy.findByRole('status').should('have.text', 'Recording. Speak now.')
      })
      return this
    },
    stopRecording() {
      audioToolsPanel().within(() => {
        cy.findByRole('button', { name: 'Stop' }).click()
        cy.findByRole('status', { timeout: 30000 })
          .should('not.have.text', 'Recording. Speak now.')
          .and('not.have.text', 'Turning your speech into text…')
      })
      waitUntilAppIsNotBusy()
      return this
    },
    expectAddedToNote() {
      audioToolsPanel()
        .findByRole('status')
        .should('have.text', 'Added to your note.')
      return this
    },
    expectConversionFailureWithRetry() {
      cy.findByText(conversionFailure).should('be.visible')
      cy.findByRole('button', { name: 'Retry' }).should('not.be.disabled')
      return this
    },
    retry() {
      cy.findByRole('button', { name: 'Retry' }).click()
      return this
    },
    expectNoConversionFailure() {
      cy.findByText(conversionFailure).should('not.exist')
      cy.findByRole('button', { name: 'Retry' }).should('not.exist')
      return this
    },
  }
}

export default audioToolsPage

export const assumeAudioTools = () => {
  audioToolsPanel().should('be.visible')
  return audioToolsPage()
}
