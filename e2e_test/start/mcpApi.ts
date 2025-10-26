import type { McpNoteAddDTO } from '@generated/backend/models/McpNoteAddDTO'
import type { NoteCreationDTO } from '@generated/backend/models/NoteCreationDTO'
import { McpNoteCreationControllerService } from '@generated/backend/services/McpNoteCreationControllerService'
import type { BaseHttpRequest } from '@generated/backend/core/BaseHttpRequest'
import type { ApiRequestOptions } from '@generated/backend/core/ApiRequestOptions'

// Create a capturing HTTP request to extract the request configuration from the generated service
const extractRequestConfig = (
  serviceMethod: (httpRequest: BaseHttpRequest) => any
): ApiRequestOptions => {
  let capturedConfig: ApiRequestOptions | null = null

  const capturingHttpRequest: BaseHttpRequest = {
    request: (config: ApiRequestOptions) => {
      capturedConfig = config
      return Promise.resolve() as any
    },
  } as BaseHttpRequest

  serviceMethod(capturingHttpRequest)

  if (!capturedConfig) {
    throw new Error('Failed to extract request configuration')
  }

  return capturedConfig
}

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
