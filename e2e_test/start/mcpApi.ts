import type { McpNoteAddDTO } from '@generated/backend/models/McpNoteAddDTO'
import type { NoteCreationDTO } from '@generated/backend/models/NoteCreationDTO'

const mcpApi = () => {
  return {
    createNote: (parentNote: string, noteCreationDTO: NoteCreationDTO) => {
      const makeRequest = () => {
        return cy.get('@savedMcpToken').then((token) => {
          const requestBody: McpNoteAddDTO = {
            parentNote,
            noteCreationDTO,
          }
          const req = {
            method: 'POST',
            url: `/api/mcp/notes/create`,
            headers: {
              Authorization: `Bearer ${token}`,
            },
            body: requestBody,
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
