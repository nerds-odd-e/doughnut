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

describe("ManagedApi", () => {
  const apiStatus: ApiStatus = { states: [], errors: [] }
  const managedApi = new ManagedApi(apiStatus)
  const baseUrl = "http://localhost:9081"

  beforeEach(() => {
    fetchMock.resetMocks()
    apiStatus.states = []
    apiStatus.errors = []
    mockToast.error.mockClear()
  })

  describe("loading state management", () => {
    it("sets loading state during API calls", async () => {
      let loadingStateDuringCall = false
      fetchMock.mockResponse(
        () => {
          loadingStateDuringCall = apiStatus.states.length > 0
          return Promise.resolve(JSON.stringify({}))
        },
        { url: `${baseUrl}/api/user` }
      )

      await managedApi.services.getUserProfile()

      expect(loadingStateDuringCall).toBe(true)
      expect(apiStatus.states.length).toBe(0)
    })

    it("does not set loading state in silent mode", async () => {
      let loadingStateDuringCall = false
      fetchMock.mockResponse(
        () => {
          loadingStateDuringCall = apiStatus.states.length > 0
          return Promise.resolve(JSON.stringify({}))
        },
        { url: `${baseUrl}/api/user` }
      )

      await managedApi.silent.services.getUserProfile()

      expect(loadingStateDuringCall).toBe(false)
    })
  })

  describe("error handling", () => {
    it("shows error toast on API errors", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 500,
      })

      try {
        await managedApi.services.getUserProfile()
      } catch {
        // ignore
      }

      expect(mockToast.error).toHaveBeenCalled()
    })

    it("does not show error toast in silent mode", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 500,
      })

      try {
        await managedApi.silent.services.getUserProfile()
      } catch {
        // ignore
      }

      expect(mockToast.error).not.toHaveBeenCalled()
    })

    it("enhances 404 errors with method and URL", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 404,
      })

      let caughtError: Error | undefined
      try {
        await managedApi.services.getUserProfile()
      } catch (error) {
        caughtError = error as Error
      }

      expect(caughtError?.message).toContain("[404 Not Found]")
      expect(caughtError?.message).toContain("GET")
      expect(caughtError?.message).toContain("/api/user")
      expect(mockToast.error).toHaveBeenCalledWith(
        expect.stringContaining("[404 Not Found]"),
        expect.objectContaining({
          timeout: 15000,
          closeOnClick: false,
        })
      )
    })

    it("uses shorter timeout for non-404 errors", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 500,
      })

      try {
        await managedApi.services.getUserProfile()
      } catch {
        // ignore
      }

      expect(mockToast.error).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          timeout: 3000,
        })
      )
    })
  })
})
