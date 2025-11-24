import type { McpNoteAddDto } from '@generated/backend'
import type { NoteCreationDto } from '@generated/backend'
import { McpNoteCreationController } from '@generated/backend/sdk.gen'

type ApiResponse = {
  status: number
  body: unknown
}

const mcpApi = () => {
  return {
    createNote: (parentNote: string, noteCreationDto: NoteCreationDto) => {
      const makeRequest = () => {
        const requestBody: McpNoteAddDto = {
          parentNote,
          noteCreationDTO: noteCreationDto,
        }

        return cy.get('@savedMcpToken').then((token) => {
          // Call the service with token in headers for this request
          // The CancelablePromise wraps cy.then() internally, so we need to wrap it
          const authToken = typeof token === 'string' ? token : String(token)
          const promise = McpNoteCreationController.createNoteViaMcp({
            body: requestBody,
            headers: {
              Authorization: `Bearer ${authToken}`,
            },
          } as Parameters<typeof McpNoteCreationController.createNoteViaMcp>[0])
            .then(() => {
              // Success - return 200 status
              return { status: 200, body: {} }
            })
            .catch((error) => {
              return { status: error?.status ?? 500, body: error?.body ?? {} }
            })

          // Wrap the promise to make it part of the Cypress chain
          return cy.wrap(promise)
        })
      }

      return {
        shouldBeDenied: () => {
          return makeRequest().then((response) => {
            const apiResponse = response as ApiResponse
            expect(apiResponse.status).to.eq(401)
          })
        },
        shouldBeAccepted: () => {
          return makeRequest().then((response) => {
            const apiResponse = response as ApiResponse
            expect(apiResponse.status).to.eq(200)
          })
        },
      }
    },
  }
}

export default mcpApi
