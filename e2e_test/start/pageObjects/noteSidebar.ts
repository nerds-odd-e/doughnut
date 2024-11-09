export const noteSidebar = () => {
  cy.get('aside').should('exist')

  return {
    expand: (noteTopic: string) => {
      cy.get('aside').within(() => {
        cy.findByText(noteTopic)
          .parent()
          .parent()
          .within(() => {
            cy.findByTitle('expand children').click()
          })
      })
    },
    siblingOrder: (higher: string, lower: string) => {
      cy.get('aside').within(() => {
        cy.contains(higher).parent().parent().nextAll().contains(lower)
      })
    },
    expectOrderedNotes(expectedNotes: Record<string, string>[]) {
      cy.pageIsNotLoading()
      cy.get('aside ul li .card-title').then(($els) => {
        const actualNotes = Array.from($els, (el) => el.innerText)
        const expectedNoteTopics = expectedNotes.map(
          (note) => note['note-topic']
        )

        // Check both length and order
        expect(actualNotes.length, 'Number of notes should match').to.equal(
          expectedNoteTopics.length
        )

        // Check each note is in the correct position
        actualNotes.forEach((actualNote, index) => {
          expect(actualNote, `Note at position ${index + 1}`).to.equal(
            expectedNoteTopics[index]
          )
        })
      })
    },
  }
}
