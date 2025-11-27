import { client as globalClient } from "@generated/backend/client.gen"
import type { ApiStatus } from "./ApiStatusHandler"
import ApiStatusHandler from "./ApiStatusHandler"
import assignBadRequestProperties from "./window/assignBadRequestProperties"
import loginOrRegisterAndHaltThisThread from "./window/loginOrRegisterAndHaltThisThread"
import { useToast } from "vue-toastification"

// Global apiStatusHandler instance (set by setupGlobalClient)
let apiStatusHandler: ApiStatusHandler | undefined

type SdkResult = {
  error?: unknown
  response?: { url?: string; status?: number }
  request?: { method?: string; url?: string }
}

/**
 * Wrapper for API calls that manages loading state and enables error handling.
 * Use this when you need the loading state to be set immediately before the API call.
 *
 * IMPORTANT: Only calls wrapped with this function will show error toasts and handle 401 redirects.
 * Non-wrapped calls are "silent" and will not trigger error UI.
 *
 * @param apiCall - Function that returns a Promise with the API result (SDK format with error, response, request)
 * @returns Promise with the API result
 *
 * @example
 * const result = await apiCallWithLoading(() =>
 *   someApiCall({ path: { id: 123 } })
 * )
 */
export async function apiCallWithLoading<T extends SdkResult>(
  apiCall: () => Promise<T>
): Promise<T> {
  if (!apiStatusHandler) {
    return await apiCall()
  }

  apiStatusHandler.assignLoading(true)
  try {
    const result = await apiCall()

    // Handle error if present in the SDK result
    if (result.error) {
      handleSdkError(result)
    }

    return result
  } finally {
    apiStatusHandler.assignLoading(false)
  }
}

/**
 * Sets up the global client.
 * This should be called once during app initialization (e.g., in DoughnutApp.vue).
 */
export function setupGlobalClient(apiStatus: ApiStatus) {
  apiStatusHandler = new ApiStatusHandler(apiStatus)

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
}

/**
 * Handles API errors from SDK result format.
 * Extracted from ManagedApi.handleApiError.
 * @param result - SDK result with { error, response, request }
 */
function handleSdkError(result: SdkResult) {
  if (!apiStatusHandler) return
  if (!result.error) return

  const status = result.response?.status
  const url = result.response?.url
  const method = result.request?.method
  const errorBody = result.error

  // Handle 401 unauthorized - redirect to login
  if (status === 401) {
    if (
      method === "GET" ||
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
  let msg = "API Error"
  if (
    errorBody &&
    typeof errorBody === "object" &&
    "message" in errorBody &&
    typeof errorBody.message === "string"
  ) {
    msg = errorBody.message
  } else if (typeof errorBody === "string") {
    msg = errorBody
  }

  // For 404 errors, include endpoint details for better debugging
  if (status === 404) {
    const enhancedMsg = `[404 Not Found] ${method || "UNKNOWN"} ${url || "UNKNOWN"}\n\n${msg}`
    msg = enhancedMsg
  }

  // Show error toast
  const toast = useToast()
  // For 404 errors, show longer timeout and make it more visible
  const timeout = status === 404 ? 15000 : 3000 // 15 seconds for 404, 3 seconds for others
  toast.error(msg, {
    timeout,
    closeOnClick: false, // Prevent accidental dismissal for 404 errors
    pauseOnFocusLoss: true,
    pauseOnHover: true,
  })

  // Handle 400 bad request - assign properties to error object
  if (status === 400 && errorBody) {
    const jsonResponse =
      typeof errorBody === "string" ? JSON.parse(errorBody) : errorBody
    assignBadRequestProperties(errorBody as Error, jsonResponse)
  }
}
