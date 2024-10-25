const audioToolsPage = () => {
  return {
    startRecording: () => {
      cy.findByRole('button', { name: 'Record Audio' }).click()
      cy.findByRole('button', { name: 'Record Audio' }).should('be.disabled')
      return this
    },
    stopRecording: () => {
      cy.findByRole('button', { name: 'Stop Recording' }).click()
      return this
    },
    startToUploadAudioFile: (fileName: string) => {
      cy.get('#note-uploadAudioFile').attachFile(fileName)
      return this
    },
    downloadAudioFile(fileName: string) {
      const downloadsFolder = Cypress.config('downloadsFolder')
      cy.findByRole('button', { name: `Save Audio Locally` }).click()
      cy.task('fileShouldExistSoon', `${downloadsFolder}/${fileName}`).should(
        'equal',
        true
      )
    },
  }
}

export default audioToolsPage

export const assumeAudioTools = () => {
  cy.findByRole('button', { name: 'Record Audio' }).should('exist')
  return audioToolsPage()
}
