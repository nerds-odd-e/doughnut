import { waitUntilAppIsNotBusy } from '../pageBase'

const conversionFailure =
  'Could not turn your speech into text. Your recording is kept until you close Audio tools.'

const audioToolsPanel = () => cy.findByRole('region', { name: 'Audio tools' })

const audioToolsPage = () => {
  return {
    startRecording() {
      audioToolsPanel().within(() => {
        cy.findByRole('button', { name: 'Record' }).click()
        cy.findByRole('button', { name: 'Stop' }).should('be.visible')
      })
      return this
    },
    stopRecording() {
      audioToolsPanel().within(() => {
        cy.findByRole('button', { name: 'Stop' }).click()
        cy.findByRole('button', { name: 'Record', timeout: 30000 }).should(
          'be.enabled'
        )
      })
      waitUntilAppIsNotBusy()
      return this
    },
    expectConversionFailureWithRetry() {
      audioToolsPanel().within(() => {
        cy.findByText(conversionFailure).should('be.visible')
        cy.findByRole('button', { name: 'Retry' }).should('not.be.disabled')
        cy.findByRole('button', { name: 'Record' }).should('be.visible')
      })
      return this
    },
    retry() {
      audioToolsPanel().within(() => {
        cy.findByRole('button', { name: 'Retry' }).click()
        cy.findByRole('button', { name: 'Retry', timeout: 30000 }).should(
          'not.exist'
        )
      })
      waitUntilAppIsNotBusy()
      return this
    },
  }
}

export default audioToolsPage

export const assumeAudioTools = () => {
  audioToolsPanel().should('be.visible')
  return audioToolsPage()
}
