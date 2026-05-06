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
            throwOnError: false,
            responseStyle: 'fields',
          } as Parameters<typeof UserController.getTokenInfo>[0]).then(
            (result) => {
              const res = result as {
                response?: Response
                data?: unknown
                error?: unknown
              }
              if (res.response?.ok) {
                return { status: 200, body: res.data ?? {} }
              }
              return {
                status: res.response?.status ?? 500,
                body: res.error ?? {},
              }
            }
          )

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
