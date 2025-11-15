import { OpenAPI, ApiError, CancelablePromise } from "@generated/backend"
import * as Services from "@generated/backend/services.gen"
import type { ApiStatus } from "./ApiStatusHandler"
import ApiStatusHandler from "./ApiStatusHandler"
import assignBadRequestProperties from "./window/assignBadRequestProperties"
import loginOrRegisterAndHaltThisThread from "./window/loginOrRegisterAndHaltThisThread"

// Set up OpenAPI config
OpenAPI.BASE = ""
OpenAPI.VERSION = "0"
OpenAPI.WITH_CREDENTIALS = true
OpenAPI.CREDENTIALS = "include"

class ManagedApi {
  apiStatus: ApiStatus

  apiStatusHandler: ApiStatusHandler

  silentApi: ManagedApi | undefined = undefined

  private isSilent: boolean

  public readonly services: typeof Services

  constructor(apiStatus: ApiStatus, silent?: boolean) {
    this.apiStatus = apiStatus
    this.isSilent = silent ?? false
    this.apiStatusHandler = new ApiStatusHandler(apiStatus, silent)

    // Wrap Services object with Proxy for error handling and loading states
    this.services = this.wrapServices()
  }

  // Helper to wrap Services object for error handling and loading states
  protected wrapServices(): typeof Services {
    const self = this
    const wrapped: Record<string, unknown> = {}

    // Wrap all service functions
    for (const key in Services) {
      if (Object.hasOwn(Services, key)) {
        const value = (Services as Record<string, unknown>)[key]
        if (typeof value === "function") {
          const originalFn = value as (
            ...args: unknown[]
          ) => CancelablePromise<unknown>
          wrapped[key] = (...args: unknown[]) => {
            return self.wrapServiceCall(() => originalFn(...args))
          }
        } else {
          wrapped[key] = value
        }
      }
    }

    return wrapped as typeof Services
  }

  // Helper to wrap generated function calls with error handling and loading states
  // This is used by service instances to handle ApiError and manage loading states
  wrapServiceCall<T>(fn: () => CancelablePromise<T>): CancelablePromise<T> {
    return new CancelablePromise<T>(async (resolve, reject, onCancel) => {
      // Set loading state
      if (!this.isSilent) {
        this.apiStatusHandler.assignLoading(true)
      }

      try {
        const originalPromise = fn()
        onCancel(() => originalPromise.cancel())
        const result = await originalPromise

        // Clear loading state on success
        if (!this.isSilent) {
          this.apiStatusHandler.assignLoading(false)
        }

        resolve(result)
      } catch (error) {
        // Clear loading state on error
        if (!this.isSilent) {
          this.apiStatusHandler.assignLoading(false)
        }

        // ApiError is thrown by the generated code when the response is not OK
        if (error instanceof ApiError) {
          this.handleApiError(error)
        }
        reject(error)
      }
    })
  }

  private handleApiError(error: ApiError) {
    if (this.isSilent) return

    if (error.status === 401) {
      if (
        error.request.method === "GET" ||
        // eslint-disable-next-line no-alert
        window.confirm(
          "You are logged out. Do you want to log in (and lose the current changes)?"
        )
      ) {
        loginOrRegisterAndHaltThisThread()
        return
      }
    }

    let msg =
      error.body &&
      typeof error.body === "object" &&
      "message" in error.body &&
      typeof error.body.message === "string"
        ? error.body.message
        : error.message

    // For 404 errors, include endpoint details for better debugging
    if (error.status === 404) {
      const method = error.request.method || "UNKNOWN"
      const url = error.url || error.request.url || "UNKNOWN"
      const enhancedMsg = `[404 Not Found] ${method} ${url}\n\n${msg}`
      Object.defineProperty(error, "message", {
        value: enhancedMsg,
        writable: true,
        configurable: true,
      })
      msg = enhancedMsg
    }

    this.apiStatusHandler.addError(msg, error.status)

    if (error.status === 400) {
      const jsonResponse =
        typeof error.body === "string" ? JSON.parse(error.body) : error.body
      assignBadRequestProperties(error, jsonResponse)
    }
  }

  get silent(): ManagedApi {
    // for testability reasons, we need to be able to create a silent api object
    // and memoize it. So that the method can be mocked
    if (!this.silentApi) {
      this.silentApi = new ManagedApi(this.apiStatus, true)
    }
    return this.silentApi
  }

  async logout() {
    await fetch("/logout", {
      method: "POST",
    })
  }
}

export default ManagedApi
export type { ApiStatus, ApiError }
