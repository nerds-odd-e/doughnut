import { createClient } from "@generated/backend/client"
import { client as globalClient } from "@generated/backend/client.gen"
import type { ApiStatus } from "./ApiStatusHandler"
import ApiStatusHandler from "./ApiStatusHandler"
import assignBadRequestProperties from "./window/assignBadRequestProperties"
import loginOrRegisterAndHaltThisThread from "./window/loginOrRegisterAndHaltThisThread"

// Create silent client instance (no interceptors, no loading/error UI)
// Use 'fields' and throwOnError: false to match ManagedApi's response format: { data, error, request, response }
export const globalClientSilent = createClient({
  baseUrl: typeof window !== "undefined" ? window.location.origin : "",
  credentials: "include",
  responseStyle: "fields",
  throwOnError: false,
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
  // Use 'fields' and throwOnError: false to match ManagedApi's response format: { data, error, request, response }
  globalClient.setConfig({
    baseUrl: typeof window !== "undefined" ? window.location.origin : "",
    credentials: "include",
    responseStyle: "fields",
    throwOnError: false,
  })

  // Request interceptor: Set loading state
  globalClient.interceptors.request.use(async (request) => {
    apiStatusHandler?.assignLoading(true)
    return request
  })

  // Response interceptor: Clear loading state and handle errors
  // Note: With throwOnError: false, errors are returned in the response object, not thrown
  globalClient.interceptors.response.use(async (response) => {
    apiStatusHandler?.assignLoading(false)

    // Check if response has an error field (when throwOnError is false)
    // Response type: { data, error, request, response }
    if (
      response &&
      typeof response === "object" &&
      "error" in response &&
      response.error
    ) {
      const responseObj = response as {
        error: unknown
        request?: Request
        response?: Response
      }

      const errorObj = responseObj.error as Error & {
        status?: number
        body?: unknown
        request?: { method?: string; url?: string }
        url?: string
      }

      // Extract status and body from response if available
      if (responseObj.response) {
        if (!errorObj.status) {
          errorObj.status = responseObj.response.status
        }
      }
      if (responseObj.request) {
        if (!errorObj.url) {
          errorObj.url = responseObj.request.url
        }
        if (!errorObj.request) {
          errorObj.request = {
            method: responseObj.request.method || "UNKNOWN",
            url: responseObj.request.url || "UNKNOWN",
          }
        }
      }

      handleApiError(errorObj)
    }

    return response
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
