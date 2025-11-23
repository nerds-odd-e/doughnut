import "vitest-fetch-mock"
import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import { globalClientSilent, setupGlobalClient } from "@/managedApi/clientSetup"
import { client as globalClient } from "@generated/backend/client.gen"
import { getUserProfile } from "@generated/backend/sdk.gen"
import { vi } from "vitest"

const mockToast = {
  error: vi.fn(),
}

vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))

describe("clientSetup", () => {
  const apiStatus: ApiStatus = { states: [], errors: [] }
  const baseUrl = "http://localhost:9081"

  beforeEach(() => {
    fetchMock.resetMocks()
    apiStatus.states = []
    apiStatus.errors = []
    mockToast.error.mockClear()
    // Setup global client before each test
    setupGlobalClient(apiStatus)
  })

  describe("globalClientSilent - loading state management", () => {
    it("does not set loading state when using globalClientSilent", async () => {
      let loadingStateDuringCall = false
      fetchMock.mockResponse(
        () => {
          loadingStateDuringCall = apiStatus.states.length > 0
          return Promise.resolve(JSON.stringify({ user: {} }))
        },
        { url: `${baseUrl}/api/user` }
      )

      await getUserProfile({ client: globalClientSilent })

      expect(loadingStateDuringCall).toBe(false)
      expect(apiStatus.states.length).toBe(0)
    })
  })

  describe("globalClientSilent - error handling", () => {
    it("does not show error toast when using globalClientSilent", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 500,
      })

      const { error } = await getUserProfile({ client: globalClientSilent })

      // Error should be returned but no toast should be shown
      expect(error).toBeDefined()
      expect(mockToast.error).not.toHaveBeenCalled()
    })
  })

  describe("globalClient (non-silent) - loading state management", () => {
    it("sets loading state during API calls", async () => {
      let loadingStateDuringCall = false
      fetchMock.mockResponse(
        () => {
          loadingStateDuringCall = apiStatus.states.length > 0
          return Promise.resolve(JSON.stringify({ user: {} }))
        },
        { url: `${baseUrl}/api/user` }
      )

      await getUserProfile({ client: globalClient })

      expect(loadingStateDuringCall).toBe(true)
      expect(apiStatus.states.length).toBe(0)
    })
  })

  describe("globalClient (non-silent) - error handling", () => {
    it("shows error toast on API errors", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 500,
      })

      const { error } = await getUserProfile({ client: globalClient })

      expect(error).toBeDefined()
      expect(mockToast.error).toHaveBeenCalled()
    })

    it("enhances 404 errors with method and URL", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 404,
      })

      const { error } = await getUserProfile({ client: globalClient })

      expect(error).toBeDefined()
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

      const { error } = await getUserProfile({ client: globalClient })

      expect(error).toBeDefined()
      expect(mockToast.error).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          timeout: 3000,
        })
      )
    })
  })
})
