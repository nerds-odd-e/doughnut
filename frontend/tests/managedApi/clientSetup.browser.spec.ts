import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import { apiCallWithLoading, setupGlobalClient } from "@/managedApi/clientSetup"
import { UserController } from "@generated/backend/sdk.gen"
import { beforeEach, describe, expect, it, vi } from "vitest"
import createFetchMock from "vitest-fetch-mock"

const fetchMock = createFetchMock(vi)
fetchMock.enableMocks()

const mockToast = {
  error: vi.fn(),
}

vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))

describe("clientSetup", () => {
  const apiStatus: ApiStatus = { states: [] }
  const baseUrl = "http://localhost:9081"

  beforeEach(() => {
    fetchMock.resetMocks()
    apiStatus.states = []
    mockToast.error.mockClear()
    // Setup global client before each test
    setupGlobalClient(apiStatus)
  })

  describe("error handling - silent vs with-loading behavior", () => {
    it("shows error toast for apiCallWithLoading wrapped calls", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 500,
      })

      const { error } = await apiCallWithLoading(() =>
        UserController.getUserProfile({})
      )

      expect(error).toBeDefined()
      expect(mockToast.error).toHaveBeenCalled()
    })

    it("does NOT show error toast for non-wrapped (silent) calls", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 500,
      })

      const { error } = await UserController.getUserProfile({})

      expect(error).toBeDefined()
      expect(mockToast.error).not.toHaveBeenCalled()
    })

    it("does NOT show error toast for 404 errors in wrapped calls", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 404,
      })

      const { error } = await apiCallWithLoading(() =>
        UserController.getUserProfile({})
      )

      expect(error).toBeDefined()
      expect(mockToast.error).not.toHaveBeenCalled()
    })

    it("does NOT show 404 errors for non-wrapped (silent) calls", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 404,
      })

      const { error } = await UserController.getUserProfile({})

      expect(error).toBeDefined()
      expect(mockToast.error).not.toHaveBeenCalled()
    })

    it("uses 3 second timeout for non-404 errors in wrapped calls", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 500,
      })

      const { error } = await apiCallWithLoading(() =>
        UserController.getUserProfile({})
      )

      expect(error).toBeDefined()
      expect(mockToast.error).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          timeout: 3000,
        })
      )
    })

    it("handles nested apiCallWithLoading correctly", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 500,
      })

      const result = await apiCallWithLoading(async () => {
        // Inner call should also show errors
        return await apiCallWithLoading(() => UserController.getUserProfile({}))
      })

      // Should show error toast for the inner call
      expect(result.error).toBeDefined()
      expect(mockToast.error).toHaveBeenCalled()
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

      // Both should increment loading state (2 from apiCallWithLoading)
      expect(apiStatus.states.length).toBe(2)

      await Promise.all([promise1, promise2])

      // All should be cleared
      expect(apiStatus.states.length).toBe(0)
    })
  })
})
