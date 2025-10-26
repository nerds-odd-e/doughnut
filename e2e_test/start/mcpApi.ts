import type { McpNoteAddDTO } from '@generated/backend/models/McpNoteAddDTO'
import type { NoteCreationDTO } from '@generated/backend/models/NoteCreationDTO'
import { McpNoteCreationControllerService } from '@generated/backend/services/McpNoteCreationControllerService'
import { extractRequestConfig } from './utils/apiConfigExtractor'

const mcpApi = () => {
  return {
    createNote: (parentNote: string, noteCreationDTO: NoteCreationDTO) => {
      const makeRequest = () => {
        return cy.get('@savedMcpToken').then((token) => {
          const requestBody: McpNoteAddDTO = {
            parentNote,
            noteCreationDTO,
          }

          // Extract the request configuration from the generated service
          const config = extractRequestConfig((httpRequest) => {
            const service = new McpNoteCreationControllerService(httpRequest)
            return service.createNote1(requestBody)
          })

          const req = {
            method: config.method,
            url: config.url,
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
