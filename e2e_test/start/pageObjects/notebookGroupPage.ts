const notebookGroupPage = () => ({
  expectGroupWithNotebookListed(groupName: string, notebookName: string) {
    cy.url().should(($url) => {
      expect(
        $url,
        `Expected notebook group page URL for "${groupName}", but was ${$url}`
      ).to.match(/\/notebooks\/groups\/\d+/)
    })
    cy.contains('h1', groupName).should(($heading) => {
      const actual = $heading.text().trim()
      expect(
        actual,
        `Expected notebook group page heading "${groupName}", but found "${actual}"`
      ).to.equal(groupName)
    })
    cy.get('main').within(() => {
      cy.contains('h5', notebookName).should('be.visible')
    })
    return this
  },
})

export default notebookGroupPage
