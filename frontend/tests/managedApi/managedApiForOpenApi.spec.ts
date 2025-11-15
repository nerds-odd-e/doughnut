import "vitest-fetch-mock"
import type { ApiStatus } from "@/managedApi/ManagedApi"
import ManagedApi from "@/managedApi/ManagedApi"
import { vi } from "vitest"

const mockToast = {
  error: vi.fn(),
}

vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))

describe("managdApi", () => {
  const apiStatus: ApiStatus = { states: [], errors: [] }
  const managedApi = new ManagedApi(apiStatus)
  const baseUrl = "http://localhost:9081"

  beforeEach(() => {
    fetchMock.resetMocks()
    apiStatus.states = []
    apiStatus.errors = []
    mockToast.error.mockClear()
  })

  describe("set the loading status", () => {
    it("should set the loading status", async () => {
      let interimStateLength = 0
      fetchMock.mockResponse(
        () => {
          interimStateLength = apiStatus.states.length
          return Promise.resolve("")
        },
        {
          url: `${baseUrl}/api/user`,
        }
      )
      await managedApi.services.getUserProfile()
      expect(interimStateLength).toBeGreaterThan(0)
      expect(apiStatus.states.length).toBe(0)
    })

    it("should not set the loading status in silent mode", async () => {
      let interimStateLength = 0
      fetchMock.mockResponse(
        () => {
          interimStateLength = apiStatus.states.length
          return Promise.resolve("")
        },
        {
          url: `${baseUrl}/api/user`,
        }
      )
      await managedApi.silent.services.getUserProfile()
      expect(interimStateLength).toBe(0)
    })
  })

  describe("error handling", () => {
    beforeEach(() => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 404,
      })
    })

    const callApiAndIgnoreError = async () => {
      try {
        await managedApi.services.getUserProfile()
      } catch (_e) {
        // ignore
      }
    }

    it("should show error toast", async () => {
      await callApiAndIgnoreError()
      expect(mockToast.error).toHaveBeenCalled()
    })

    it("should not show error toast in silent mode", async () => {
      try {
        await managedApi.silent.services.getUserProfile()
      } catch (_e) {
        // ignore
      }
      expect(mockToast.error).not.toHaveBeenCalled()
    })

    describe("404 error enhancement", () => {
      it("should enhance 404 error message with method and URL in toast", async () => {
        fetchMock.mockResponse(JSON.stringify({}), {
          url: `${baseUrl}/api/test/endpoint`,
          status: 404,
        })

        try {
          await managedApi.services.getUserProfile()
        } catch (_e) {
          // ignore
        }

        expect(mockToast.error).toHaveBeenCalledWith(
          expect.stringContaining("[404 Not Found]"),
          expect.objectContaining({
            timeout: 15000,
            closeOnClick: false,
          })
        )

        const errorCall = mockToast.error.mock.calls[0]
        expect(errorCall).toBeDefined()
        expect(errorCall![0]).toContain("GET")
        expect(errorCall![0]).toContain("/api/user")
      })

      it("should enhance error.message property for Cypress visibility", async () => {
        fetchMock.mockResponse(JSON.stringify({}), {
          url: `${baseUrl}/api/missing/endpoint`,
          status: 404,
        })

        let caughtError: Error | undefined
        try {
          await managedApi.services.getUserProfile()
        } catch (error) {
          caughtError = error as Error
        }

        expect(caughtError).toBeDefined()
        expect(caughtError?.message).toContain("[404 Not Found]")
        expect(caughtError?.message).toContain("GET")
        expect(caughtError?.message).toContain("/api/user")
      })

      it("should use 15 second timeout for 404 errors", async () => {
        fetchMock.mockResponse(JSON.stringify({}), {
          url: `${baseUrl}/api/user`,
          status: 404,
        })

        await callApiAndIgnoreError()

        expect(mockToast.error).toHaveBeenCalledWith(
          expect.any(String),
          expect.objectContaining({
            timeout: 15000,
          })
        )
      })

      it("should disable closeOnClick for 404 errors", async () => {
        fetchMock.mockResponse(JSON.stringify({}), {
          url: `${baseUrl}/api/user`,
          status: 404,
        })

        await callApiAndIgnoreError()

        expect(mockToast.error).toHaveBeenCalledWith(
          expect.any(String),
          expect.objectContaining({
            closeOnClick: false,
          })
        )
      })
    })

    describe("non-404 error handling", () => {
      it("should use 3 second timeout for non-404 errors", async () => {
        fetchMock.mockResponse(JSON.stringify({}), {
          url: `${baseUrl}/api/user`,
          status: 500,
        })

        try {
          await managedApi.services.getUserProfile()
        } catch (_e) {
          // ignore
        }

        expect(mockToast.error).toHaveBeenCalledWith(
          expect.any(String),
          expect.objectContaining({
            timeout: 3000,
          })
        )
      })

      it("should not enhance error message for non-404 errors", async () => {
        fetchMock.mockResponse(JSON.stringify({}), {
          url: `${baseUrl}/api/user`,
          status: 500,
        })

        let caughtError: Error | undefined
        try {
          await managedApi.services.getUserProfile()
        } catch (error) {
          caughtError = error as Error
        }

        expect(caughtError).toBeDefined()
        expect(caughtError?.message).not.toContain("[404 Not Found]")
      })
    })
  })
})
