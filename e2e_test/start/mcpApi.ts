import { UserController } from '@generated/doughnut-backend-api/sdk.gen'

type ApiResponse = {
  status: number
  body: unknown
}

const mcpApi = () => {
  return {
    getTokenInfo: () => {
      const makeRequest = () => {
        return cy.get('@savedAccessToken').then((token) => {
          const authToken = typeof token === 'string' ? token : String(token)
          const promise = UserController.getTokenInfo({
            headers: {
              Authorization: `Bearer ${authToken}`,
            },
          } as Parameters<typeof UserController.getTokenInfo>[0])
            .then(() => {
              return { status: 200, body: {} }
            })
            .catch((error) => {
              return { status: error?.status ?? 500, body: error?.body ?? {} }
            })

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
