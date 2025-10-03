const mcpApi = () => {
  return {
    createNote: (
      parentNote: string,
      noteCreationDTO: { newTitle: string; wikidataId: string }
    ) => {
      return cy.get('@savedMcpToken').then((token) => {
        const req = {
          method: 'POST',
          url: `/api/mcp/notes/create`,
          headers: {
            Authorization: `Bearer ${token}`,
          },
          body: {
            parentNote: parentNote,
            noteCreationDTO: noteCreationDTO,
          },
          failOnStatusCode: false,
        }
        return cy.request(req)
      })
    },
  }
}

export default mcpApi
