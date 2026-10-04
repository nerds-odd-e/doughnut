import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import { apiCallWithLoading, setupGlobalClient } from "@/managedApi/clientSetup"
import { browserLocation } from "@/managedApi/window/browserLocation"
import {
  HealthCheckController,
  UserController,
} from "@generated/donut-backend-api/sdk.gen"
import { healthcheckPingBody, mockSdkService } from "@tests/helpers"
import {
  showToastsOnPage,
  toastOnPage,
  toastShown,
  toastTimeout,
} from "@tests/helpers/toastTestSupport"
import { flushPromises } from "@vue/test-utils"
import { page } from "vitest/browser"
import {
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest"
import createFetchMock from "vitest-fetch-mock"

const fetchMock = createFetchMock(vi)
fetchMock.enableMocks()

const noToastShown = async () => {
  await flushPromises()
  expect(toastOnPage()).toBeNull()
}

describe("clientSetup", () => {
  const apiStatus: ApiStatus = { states: [] }
  const baseUrl = "http://localhost:9081"

  showToastsOnPage()

  // Each setup adds another 401 interceptor to the shared client, so it runs once
  beforeAll(() => {
    setupGlobalClient(apiStatus)
  })

  beforeEach(() => {
    fetchMock.resetMocks()
    apiStatus.states = []
  })

  afterEach(() => {
    vi.restoreAllMocks()
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
      await toastShown("error")
    })

    it("does NOT show error toast for non-wrapped (silent) calls", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 500,
      })

      const { error } = await UserController.getUserProfile({})

      expect(error).toBeDefined()
      await noToastShown()
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
      await noToastShown()
    })

    it("does NOT show 404 errors for non-wrapped (silent) calls", async () => {
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 404,
      })

      const { error } = await UserController.getUserProfile({})

      expect(error).toBeDefined()
      await noToastShown()
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
      expect(toastTimeout(await toastShown("error"))).toBe("3000ms")
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
      await toastShown("error")
    })

    it("does not toast a late error after cancellation", async () => {
      let resolveCall: (value: {
        error: string
        response: { status: number }
      }) => void = () => undefined
      const result = apiCallWithLoading(
        () =>
          new Promise<{ error: string; response: { status: number } }>(
            (resolve) => {
              resolveCall = resolve
            }
          ),
        { blockUi: true, cancelable: true }
      )

      apiStatus.states[0]?.cancel?.()
      await expect(result).resolves.toEqual({ status: "cancelled" })
      resolveCall({ error: "request aborted", response: { status: 500 } })
      await Promise.resolve()

      await noToastShown()
    })
  })

  describe("401 unauthorized — redirect to sign-in", () => {
    const answerUnauthorizedWithPing = () => {
      fetchMock.mockResponse(JSON.stringify({}), { status: 401 })
      mockSdkService(HealthCheckController, "ping", healthcheckPingBody("test"))
      return vi
        .spyOn(browserLocation, "assign")
        .mockImplementation(() => undefined)
    }

    it("shows warning toast with API path then redirects on GET 401", async () => {
      vi.spyOn(window, "confirm").mockImplementation(() => {
        throw new Error("confirm must not be used for GET")
      })
      const assignSpy = answerUnauthorizedWithPing()

      await UserController.getUserProfile({})

      const toast = await toastShown("warning")
      await expect
        .element(
          page.getByText(
            "This page will reload to sign you in again. Reason: unauthorized response from GET /api/user."
          )
        )
        .toBeInTheDocument()
      expect(toastTimeout(toast)).toBe("8000ms")
      await vi.waitFor(() =>
        expect(assignSpy).toHaveBeenCalledWith(
          `/users/identify?from=${window.location.href}`
        )
      )
    })

    it("does not redirect when user declines login on mutating 401", async () => {
      vi.spyOn(window, "confirm").mockReturnValue(false)
      const assignSpy = answerUnauthorizedWithPing()

      await UserController.createUser({
        body: { name: "x" } as never,
      })

      await noToastShown()
      expect(assignSpy).not.toHaveBeenCalled()
    })

    it("shows warning toast and redirects when user accepts login on mutating 401", async () => {
      vi.spyOn(window, "confirm").mockReturnValue(true)
      const assignSpy = answerUnauthorizedWithPing()

      await UserController.createUser({
        body: { name: "x" } as never,
      })

      const toast = await toastShown("warning")
      await expect
        .element(
          page.getByText(
            "This page will reload to sign you in again. Reason: unauthorized response from POST /api/user."
          )
        )
        .toBeInTheDocument()
      expect(toastTimeout(toast)).toBe("8000ms")
      await vi.waitFor(() =>
        expect(assignSpy).toHaveBeenCalledWith(
          `/users/identify?from=${window.location.href}`
        )
      )
    })
  })
})
