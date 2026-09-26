import { waitUntilAppIsNotBusy } from '../pageBase'

const audioToolsPage = () => {
  return {
    startRecording() {
      cy.findByRole('button', { name: 'Record Audio' }).click()
      return this
    },
    stopRecording() {
      cy.findByRole('button', { name: 'Stop Recording' }).click()
      // Final transcription (audio-to-text) raises no busy marker; the saved file appears once it finishes.
      cy.findByRole('button', {
        name: 'Save Audio Locally',
        timeout: 30000,
      }).should('not.be.disabled')
      waitUntilAppIsNotBusy()
      return this
    },
  }
}

export default audioToolsPage

export const assumeAudioTools = () => {
  cy.findByRole('button', { name: 'Stop Recording' }).should('exist')
  return audioToolsPage()
}
