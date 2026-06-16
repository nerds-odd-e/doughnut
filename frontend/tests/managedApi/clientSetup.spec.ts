import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import { apiCallWithLoading, setupGlobalClient } from "@/managedApi/clientSetup"
import loginOrRegisterAndHaltThisThread from "@/managedApi/window/loginOrRegisterAndHaltThisThread"
import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import { beforeEach, describe, expect, it, vi } from "vitest"
import createFetchMock from "vitest-fetch-mock"

const fetchMock = createFetchMock(vi)
fetchMock.enableMocks()

const mockToast = {
  error: vi.fn(),
  warning: vi.fn(),
}

vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))

vi.mock("@/managedApi/window/loginOrRegisterAndHaltThisThread", () => ({
  default: vi.fn(),
}))

describe("clientSetup", () => {
  const apiStatus: ApiStatus = { states: [] }
  const baseUrl = "http://localhost:9081"

  beforeEach(() => {
    fetchMock.resetMocks()
    apiStatus.states = []
    mockToast.error.mockClear()
    mockToast.warning.mockClear()
    vi.mocked(loginOrRegisterAndHaltThisThread).mockClear()
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

  describe("401 unauthorized — redirect to sign-in", () => {
    it("shows warning toast with API path then redirects on GET 401", async () => {
      const confirmSpy = vi.spyOn(window, "confirm").mockImplementation(() => {
        throw new Error("confirm must not be used for GET")
      })
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 401,
      })

      await UserController.getUserProfile({})

      expect(mockToast.warning).toHaveBeenCalledWith(
        expect.stringContaining("GET /api/user"),
        expect.objectContaining({ timeout: 8000 })
      )
      expect(loginOrRegisterAndHaltThisThread).toHaveBeenCalled()
      confirmSpy.mockRestore()
    })

    it("does not redirect when user declines login on mutating 401", async () => {
      vi.spyOn(window, "confirm").mockReturnValue(false)
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 401,
      })

      await UserController.createUser({
        body: { name: "x" } as never,
      })

      expect(mockToast.warning).not.toHaveBeenCalled()
      expect(loginOrRegisterAndHaltThisThread).not.toHaveBeenCalled()
    })

    it("shows warning toast and redirects when user accepts login on mutating 401", async () => {
      vi.spyOn(window, "confirm").mockReturnValue(true)
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 401,
      })

      await UserController.createUser({
        body: { name: "x" } as never,
      })

      expect(mockToast.warning).toHaveBeenCalledWith(
        expect.stringContaining("POST /api/user"),
        expect.objectContaining({ timeout: 8000 })
      )
      expect(loginOrRegisterAndHaltThisThread).toHaveBeenCalled()
    })
  })
})
