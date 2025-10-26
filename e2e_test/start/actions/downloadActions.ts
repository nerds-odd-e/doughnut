export interface ExpectedFile {
  Filename: string
  Format: string
  Content: string
  validateMetadata?: boolean
}

export const downloadActions = {
  checkDownloadFiles() {
    return {
      hasZipFileWith(files: ExpectedFile[]) {
        return cy.task('checkDownloadedZipContent', files)
      },
    }
  },
}
