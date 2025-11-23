import { createClient } from "@generated/backend/client"
import { client as globalClient } from "@generated/backend/client.gen"
import type { ApiStatus } from "./ApiStatusHandler"
import ApiStatusHandler from "./ApiStatusHandler"
import assignBadRequestProperties from "./window/assignBadRequestProperties"
import loginOrRegisterAndHaltThisThread from "./window/loginOrRegisterAndHaltThisThread"

// Create silent client instance (no interceptors, no loading/error UI)
export const globalClientSilent = createClient({
  baseUrl: typeof window !== "undefined" ? window.location.origin : "",
  credentials: "include",
  responseStyle: "data",
  throwOnError: true,
})

// Global apiStatusHandler instance (set by setupGlobalClient)
let apiStatusHandler: ApiStatusHandler | undefined

/**
 * Sets up the global client with interceptors for loading states and error handling.
 * This should be called once during app initialization (e.g., in DoughnutApp.vue).
 */
export function setupGlobalClient(apiStatus: ApiStatus) {
  apiStatusHandler = new ApiStatusHandler(apiStatus, false)

  // Configure global client
  globalClient.setConfig({
    baseUrl: typeof window !== "undefined" ? window.location.origin : "",
    credentials: "include",
    responseStyle: "data",
    throwOnError: true,
  })

  // Request interceptor: Set loading state
  globalClient.interceptors.request.use(async (request) => {
    apiStatusHandler?.assignLoading(true)
    return request
  })

  // Response interceptor: Clear loading state on success
  globalClient.interceptors.response.use(async (response) => {
    apiStatusHandler?.assignLoading(false)
    return response
  })

  // Error interceptor: Clear loading state and handle errors
  globalClient.interceptors.error.use(async (error, response, request) => {
    apiStatusHandler?.assignLoading(false)

    // Convert error to the format expected by handleApiError
    const errorObj = error as Error & {
      status?: number
      body?: unknown
      request?: { method?: string; url?: string }
      url?: string
    }

    // Extract status and body from response if error doesn't have them
    if (!errorObj.status && response) {
      errorObj.status = response.status
    }
    if (!errorObj.url && request) {
      errorObj.url = request.url
    }
    if (!errorObj.request && request) {
      errorObj.request = {
        method: request.method,
        url: request.url,
      }
    }
    if (!errorObj.body && error) {
      // Try to extract body from error
      if (typeof error === "object" && error !== null) {
        errorObj.body = error
      } else if (typeof error === "string") {
        errorObj.body = error
      }
    }

    handleApiError(errorObj)

    // Return error to be thrown (since throwOnError is true)
    return error
  })
}

/**
 * Handles API errors with appropriate user feedback and actions.
 * This logic is extracted from ManagedApi.handleApiError for use in interceptors.
 */
function handleApiError(
  error: Error & {
    status?: number
    body?: unknown
    request?: { method?: string; url?: string }
    url?: string
  }
) {
  if (!apiStatusHandler) return

  // Handle 401 unauthorized - redirect to login
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

  // Extract error message
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

  // Show error toast
  apiStatusHandler.addError(msg, error.status)

  // Handle 400 bad request - assign properties to error object
  if (error.status === 400) {
    const jsonResponse =
      typeof error.body === "string" ? JSON.parse(error.body) : error.body
    assignBadRequestProperties(error, jsonResponse)
  }
}
