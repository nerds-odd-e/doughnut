import { createClient } from "@generated/backend/client"
import { client as globalClient } from "@generated/backend/client.gen"
import type { ApiStatus } from "./ApiStatusHandler"
import ApiStatusHandler from "./ApiStatusHandler"
import assignBadRequestProperties from "./window/assignBadRequestProperties"
import loginOrRegisterAndHaltThisThread from "./window/loginOrRegisterAndHaltThisThread"

// Create silent client instance (no interceptors, no loading/error UI)
// Use 'fields' and throwOnError: false to match ManagedApi's response format: { data, error, request, response }
export const globalClientSilent = createClient({
  baseUrl:
    typeof window !== "undefined" && window.location.origin
      ? window.location.origin
      : "http://localhost:9081",
  credentials: "include",
  responseStyle: "fields",
  throwOnError: false,
})

// Global apiStatusHandler instance (set by setupGlobalClient)
let apiStatusHandler: ApiStatusHandler | undefined

/**
 * Wrapper for API calls that manages loading state.
 * Use this when you need the loading state to be set immediately before the API call.
 * The API call will use the default globalClient unless explicitly overridden.
 *
 * @param apiCall - Function that returns a Promise with the API result
 * @returns Promise with the API result
 *
 * @example
 * const result = await apiCallWithLoading(() =>
 *   someApiCall({ path: { id: 123 } })
 * )
 */
export async function apiCallWithLoading<T>(
  apiCall: () => Promise<T>
): Promise<T> {
  if (!apiStatusHandler) {
    return await apiCall()
  }

  apiStatusHandler.assignLoading(true)
  try {
    return await apiCall()
  } finally {
    apiStatusHandler.assignLoading(false)
  }
}

/**
 * Sets up the global client with interceptors for loading states and error handling.
 * This should be called once during app initialization (e.g., in DoughnutApp.vue).
 */
export function setupGlobalClient(apiStatus: ApiStatus) {
  apiStatusHandler = new ApiStatusHandler(apiStatus, false)

  // Configure global client
  // Use 'fields' and throwOnError: false to match ManagedApi's response format: { data, error, request, response }
  globalClient.setConfig({
    baseUrl:
      typeof window !== "undefined" && window.location.origin
        ? window.location.origin
        : "http://localhost:9081",
    credentials: "include",
    responseStyle: "fields",
    throwOnError: false,
  })

  // Request interceptor: Set loading state
  globalClient.interceptors.request.use(async (request) => {
    apiStatusHandler?.assignLoading(true)
    return request
  })

  // Error interceptor: Handle errors (toasts, 401 redirects, etc.)
  // This runs when there's an error response, before it's returned
  globalClient.interceptors.error.use(async (error, response, request) => {
    // Construct error object for handleApiError
    // The error parameter is the parsed response body (object or string)
    const errorBody = error
    const errorObj = new Error(
      typeof errorBody === "string"
        ? errorBody
        : typeof errorBody === "object" &&
            errorBody !== null &&
            "message" in errorBody &&
            typeof errorBody.message === "string"
          ? errorBody.message
          : "API Error"
    ) as Error & {
      status?: number
      body?: unknown
      request?: { method?: string; url?: string }
      url?: string
    }

    // Set status from response
    if (response) {
      errorObj.status = response.status
      errorObj.url = response.url
    }

    // Set request info
    if (request) {
      errorObj.request = {
        method: request.method || "UNKNOWN",
        url: request.url || "UNKNOWN",
      }
    }

    // Set body - the error parameter IS the body
    errorObj.body = errorBody

    handleApiError(errorObj)

    // Return the error as-is (don't transform it)
    return error
  })

  // Response interceptor: Clear loading state
  globalClient.interceptors.response.use(async (response) => {
    apiStatusHandler?.assignLoading(false)
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
