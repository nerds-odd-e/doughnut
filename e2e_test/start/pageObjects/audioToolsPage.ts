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
    uploadAudioFile: function (fileName: string) {
      this.startToUploadAudioFile(fileName)
      cy.findAllByText('Save').click()
      cy.pageIsNotLoading()
      return this
    },
  }
}

export default audioToolsPage
