import { waitUntilAppIsNotBusy } from '../pageBase'

const audioToolsPage = () => {
  return {
    startRecording() {
      cy.findByRole('button', { name: 'Record Audio' }).click()
      return this
    },
    stopRecording() {
      cy.findByRole('button', { name: 'Stop Recording' }).click()
      waitUntilAppIsNotBusy()
      cy.findByRole('button', { name: 'Save Audio Locally' }).should(
        'not.be.disabled'
      )
      return this
    },
    startToUploadAudioFile(fileName: string) {
      cy.get('#note-uploadAudioFile').attachFile(fileName)
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
