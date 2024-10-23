const audioToolsPage = () => {
  return {
    startRecording: () => {
      cy.findByText('Record Audio').click()
      return this
    },
    stopRecording: () => {
      cy.findByText('Stop Recording').click()
      return this
    },
    startToUploadAudioFile: (fileName: string) => {
      cy.get('#note-uploadAudioFile').attachFile(fileName)
      return this
    },
    downloadAudioFile(fileName: string) {
      const downloadsFolder = Cypress.config('downloadsFolder')
      cy.findByRole('button', { name: `Download ${fileName}` }).click()
      cy.task('fileShouldExistSoon', `${downloadsFolder}/${fileName}`).should(
        'equal',
        true
      )
    },
  }
}

export default audioToolsPage

export const assumeAudioTools = () => {
  cy.findByText('Record Audio').should('exist')
  return audioToolsPage()
}
