import "vitest-fetch-mock"
import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import { apiCallWithLoading, setupGlobalClient } from "@/managedApi/clientSetup"
import { client as globalClient } from "@generated/backend/client.gen"
import { UserController } from "@generated/backend/sdk.gen"
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

      await UserController.getUserProfile({ client: globalClient })

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

      const { error } = await UserController.getUserProfile({
        client: globalClient,
      })

      expect(error).toBeDefined()
      expect(mockToast.error).toHaveBeenCalled()
    })

    it("enhances 404 errors with method and URL", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 404,
      })

      const { error } = await UserController.getUserProfile({
        client: globalClient,
      })

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

      const { error } = await UserController.getUserProfile({
        client: globalClient,
      })

      expect(error).toBeDefined()
      expect(mockToast.error).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          timeout: 3000,
        })
      )
    })
  })

  describe("apiCallWithLoading - loading state management", () => {
    it("sets loading state synchronously before API call", async () => {
      let loadingStateBeforeCall = false
      let loadingStateDuringCall = false

      fetchMock.mockResponse(
        () => {
          loadingStateDuringCall = apiStatus.states.length > 0
          return Promise.resolve(JSON.stringify({ user: {} }))
        },
        { url: `${baseUrl}/api/user` }
      )

      // Check loading state is set synchronously
      const promise = apiCallWithLoading(() => {
        loadingStateBeforeCall = apiStatus.states.length > 0
        return UserController.getUserProfile({})
      })

      // Loading should be set immediately (synchronously)
      expect(apiStatus.states.length).toBe(1)

      await promise

      expect(loadingStateBeforeCall).toBe(true)
      expect(loadingStateDuringCall).toBe(true)
      expect(apiStatus.states.length).toBe(0)
    })

    it("clears loading state after successful API call", async () => {
      fetchMock.mockResponse(JSON.stringify({ user: {} }), {
        url: `${baseUrl}/api/user`,
      })

      await apiCallWithLoading(() => UserController.getUserProfile({}))

      expect(apiStatus.states.length).toBe(0)
    })

    it("clears loading state even on API error", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 500,
      })

      await apiCallWithLoading(() => UserController.getUserProfile({}))

      expect(apiStatus.states.length).toBe(0)
    })

    it("returns the API result correctly", async () => {
      const mockUser = { name: "Test User" }
      fetchMock.mockResponse(JSON.stringify(mockUser), {
        url: `${baseUrl}/api/user`,
        headers: { "Content-Type": "application/json" },
      })

      const { data } = await apiCallWithLoading(() =>
        UserController.getUserProfile({})
      )

      expect(data).toEqual(mockUser)
    })

    it("handles multiple concurrent calls correctly", async () => {
      fetchMock.mockResponse(JSON.stringify({ user: {} }), {
        url: `${baseUrl}/api/user`,
      })

      // Start two concurrent calls
      const promise1 = apiCallWithLoading(() =>
        UserController.getUserProfile({})
      )
      const promise2 = apiCallWithLoading(() =>
        UserController.getUserProfile({})
      )

      // Both should increment loading state (2 from apiCallWithLoading + 2 from interceptor)
      expect(apiStatus.states.length).toBeGreaterThanOrEqual(2)

      await Promise.all([promise1, promise2])

      // All should be cleared
      expect(apiStatus.states.length).toBe(0)
    })
  })
})
