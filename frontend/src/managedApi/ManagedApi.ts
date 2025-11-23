// Note: The new client throws errors directly, not wrapped in ApiError
// We'll handle errors generically
import { createClient } from "@generated/backend/client"
import * as Services from "@generated/backend/sdk.gen"
import type { ApiStatus } from "./ApiStatusHandler"
import ApiStatusHandler from "./ApiStatusHandler"
import { globalClientSilent } from "./clientSetup"
import assignBadRequestProperties from "./window/assignBadRequestProperties"
import loginOrRegisterAndHaltThisThread from "./window/loginOrRegisterAndHaltThisThread"

// Type helper to unwrap all service functions
// Functions that return promises with wrapped responses get unwrapped to return just the data
type UnwrappedServices<T> = {
  [K in keyof T]: T[K] extends (...args: infer Args) => infer Return
    ? Return extends Promise<infer R>
      ? R extends {
          data: infer D
          error?: unknown
          request?: unknown
          response?: unknown
        }
        ? (...args: Args) => Promise<Exclude<D, undefined>>
        : R extends { data: infer D; error: undefined }
          ? (...args: Args) => Promise<Exclude<D, undefined>>
          : R extends { data: infer D }
            ? (...args: Args) => Promise<Exclude<D, undefined>>
            : T[K]
      : T[K]
    : T[K]
}

class ManagedApi {
  apiStatus: ApiStatus

  apiStatusHandler: ApiStatusHandler

  silentApi: ManagedApi | undefined = undefined

  private isSilent: boolean

  // Separate client instance for ManagedApi (no interceptors to prevent duplication)
  private managedApiClient = createClient({
    baseUrl: typeof window !== "undefined" ? window.location.origin : "",
    credentials: "include",
    responseStyle: "fields",
    throwOnError: false,
  })

  // Reuse silent client for silent mode
  private managedApiClientSilent = globalClientSilent

  public readonly services: UnwrappedServices<typeof Services>

  constructor(apiStatus: ApiStatus, silent?: boolean) {
    this.apiStatus = apiStatus
    this.isSilent = silent ?? false
    this.apiStatusHandler = new ApiStatusHandler(apiStatus, silent)

    // Wrap Services object with Proxy for error handling and loading states
    this.services = this.wrapServices()
  }

  // Helper to wrap Services object for error handling and loading states
  protected wrapServices(): UnwrappedServices<typeof Services> {
    const self = this
    const wrapped: Record<string, unknown> = {}

    // Wrap all service functions
    for (const key in Services) {
      if (Object.hasOwn(Services, key)) {
        const value = (Services as Record<string, unknown>)[key]
        if (typeof value === "function") {
          const originalFn = value as (...args: unknown[]) => Promise<unknown>
          wrapped[key] = (...args: unknown[]) => {
            // Pass the appropriate client to prevent duplicate interceptors
            const options = (args[0] as Record<string, unknown>) || {}
            const clientToUse = self.isSilent
              ? self.managedApiClientSilent
              : self.managedApiClient
            return self.wrapServiceCall(() =>
              originalFn({ ...options, client: clientToUse } as never)
            )
          }
        } else {
          wrapped[key] = value
        }
      }
    }

    return wrapped as UnwrappedServices<typeof Services>
  }

  // Helper to extract data from response wrapper
  // The new client returns { data, error, request, response } format by default
  private extractData<T>(
    response:
      | T
      | { data?: T; error?: unknown; request?: Request; response?: Response }
  ): T {
    if (response && typeof response === "object") {
      // Check if this is the wrapped response format
      if (
        "error" in response ||
        "data" in response ||
        "request" in response ||
        "response" in response
      ) {
        const wrapped = response as {
          data?: T
          error?: unknown
          request?: Request
          response?: Response
        }
        // If there's an error, throw it
        if ("error" in wrapped && wrapped.error !== undefined) {
          const error = wrapped.error
          const errorObj = new Error(
            typeof error === "string" ? error : "API Error"
          ) as Error & {
            status?: number
            body?: unknown
            request?: Request
            url?: string
          }
          if (wrapped.response) {
            errorObj.status = wrapped.response.status
            errorObj.url = wrapped.response.url
          }
          if (wrapped.request) {
            errorObj.request = wrapped.request
          }
          if (typeof error === "object" && error !== null) {
            errorObj.body = error
          } else if (typeof error === "string") {
            errorObj.body = error
          }
          throw errorObj
        }
        // If there's data, return it
        if ("data" in wrapped && wrapped.data !== undefined) {
          return wrapped.data
        }
      }
    }
    // If it's not wrapped, return as-is (for backward compatibility)
    return response as T
  }

  // Helper to wrap generated function calls with error handling and loading states
  // This is used by service instances to handle errors and manage loading states
  wrapServiceCall<T>(
    fn: () => Promise<T | { data: T; error?: unknown }>
  ): Promise<T> {
    return (async () => {
      // Set loading state
      if (!this.isSilent) {
        this.apiStatusHandler.assignLoading(true)
      }

      try {
        const result = await fn()

        // Clear loading state on success
        if (!this.isSilent) {
          this.apiStatusHandler.assignLoading(false)
        }

        // Unwrap the response to return just the data
        const unwrappedData = this.extractData(result)
        return unwrappedData
      } catch (error) {
        // Clear loading state on error
        if (!this.isSilent) {
          this.apiStatusHandler.assignLoading(false)
        }

        // Handle errors - the new client may throw different error types
        this.handleApiError(
          error as Error & {
            status?: number
            body?: unknown
            request?: { method?: string; url?: string }
            url?: string
          }
        )
        throw error
      }
    })()
  }

  private handleApiError(
    error: Error & {
      status?: number
      body?: unknown
      request?: { method?: string; url?: string }
      url?: string
    }
  ) {
    if (this.isSilent) return

    if (error.status === 401) {
      if (
        error.request?.method === "GET" ||
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
      const method = error.request?.method || "UNKNOWN"
      const url = error.url || error.request?.url || "UNKNOWN"
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
export type { ApiStatus }
