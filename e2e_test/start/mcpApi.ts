import type { McpNoteAddDTO } from '@generated/backend'
import type { NoteCreationDTO } from '@generated/backend'
import * as Services from '@generated/backend/services.gen'
import { OpenAPI } from '@generated/backend/core/OpenAPI'
import { ApiError } from '@generated/backend/core/ApiError'

const mcpApi = () => {
  return {
    createNote: (parentNote: string, noteCreationDTO: NoteCreationDTO) => {
      const makeRequest = () => {
        const requestBody: McpNoteAddDTO = {
          parentNote,
          noteCreationDTO,
        }

        return cy.get('@savedMcpToken').then((token) => {
          // Set token in OpenAPI config for this request
          const originalToken = OpenAPI.TOKEN
          OpenAPI.TOKEN = typeof token === 'string' ? token : String(token)

          // Call the service - it will use cy.request via our custom request function
          // The CancelablePromise wraps cy.then() internally, so we need to wrap it
          const promise = Services.createNote1({ requestBody })
            .then(() => {
              // Success - return 200 status
              return { status: 200, body: {} }
            })
            .catch((error) => {
              // Extract status from ApiError
              if (error instanceof ApiError) {
                return { status: error.status, body: error.body || {} }
              }
              throw error
            })
            .finally(() => {
              // Restore original token
              OpenAPI.TOKEN = originalToken
            })

          // Wrap the promise to make it part of the Cypress chain
          return cy.wrap(promise)
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
