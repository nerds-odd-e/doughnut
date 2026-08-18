function zipPath(notebookName: string): string {
  return `${Cypress.config('downloadsFolder')}/${notebookName}.zip`
}

function loadZipEntries(notebookName: string) {
  const filePath = zipPath(notebookName)
  return cy
    .task('fileShouldExistSoon', filePath)
    .should('equal', filePath)
    .then(() => cy.task<Record<string, string>>('readZipEntries', filePath))
}

/** Assertions against a catalog-exported notebook ZIP download. */
export const downloadedNotebookZip = (notebookName: string) => ({
  expectDownloaded() {
    const filePath = zipPath(notebookName)
    cy.task('fileShouldExistSoon', filePath).should('equal', filePath)
    cy.readFile(filePath, 'binary').then((content: string) => {
      expect(content.startsWith('PK')).to.equal(true)
    })
    return this
  },
  expectContains(entryPath: string) {
    loadZipEntries(notebookName).then((entries) => {
      expect(
        entries,
        `Expected zip for "${notebookName}" to contain "${entryPath}", but found [${Object.keys(entries).join(', ')}]`
      ).to.have.property(entryPath)
    })
    return this
  },
  expectDoesNotContain(entryPath: string) {
    loadZipEntries(notebookName).then((entries) => {
      expect(
        entries,
        `Expected zip for "${notebookName}" not to contain "${entryPath}"`
      ).to.not.have.property(entryPath)
    })
    return this
  },
  expectEntryIncludes(entryPath: string, fragment: string) {
    loadZipEntries(notebookName).then((entries) => {
      const actual = entries[entryPath]
      expect(
        actual,
        `Expected zip for "${notebookName}" to contain "${entryPath}", but found [${Object.keys(entries).join(', ')}]`
      ).to.not.equal(undefined)
      expect(
        actual,
        `Expected "${entryPath}" in zip for "${notebookName}" to include "${fragment}", but found "${actual}"`
      ).to.include(fragment)
    })
    return this
  },
})
