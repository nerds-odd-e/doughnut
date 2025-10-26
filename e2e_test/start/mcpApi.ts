const mcpApi = () => {
  return {
    createNote: (
      parentNote: string,
      noteCreationDTO: { newTitle: string; wikidataId: string }
    ) => {
      const makeRequest = () => {
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
      }

      return {
        shouldBeDenied: () => {
          return makeRequest().then((response) => {
            expect(response.status).to.eq(401)
          })
        },
        shouldBeAccepted: () => {
          return makeRequest().then((response) => {
            expect(response.status).to.eq(200)
          })
        },
      }
    },
  }
}

export default mcpApi
