/// <reference types="Cypress" />

export interface ExpectedFile {
  Filename: string
  Format: string
  Content: string
  validateMetadata?: boolean
}

const downloadChecker = () => {
  return {
    checkDownloadFiles() {
      return {
        hasZipFileWith(files: ExpectedFile[]) {
          return cy.task('checkDownloadedZipContent', files)
        },
      }
    },
  }
}

export default downloadChecker 