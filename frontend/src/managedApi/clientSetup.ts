import { client as globalClient } from "@generated/backend/client.gen"
import { createClient, type Config } from "@generated/backend/client"
import type { ApiStatus } from "./ApiStatusHandler"
import ApiStatusHandler from "./ApiStatusHandler"
import assignBadRequestProperties from "./window/assignBadRequestProperties"
import loginOrRegisterAndHaltThisThread from "./window/loginOrRegisterAndHaltThisThread"
import { useToast } from "vue-toastification"

// Global apiStatusHandler instance (set by setupGlobalClient)
let apiStatusHandler: ApiStatusHandler | undefined

// Client that does not trigger page reload on 401 errors
export const nonReloadingClient = createClient()

type SdkResult = {
  error?: unknown
  response?: { url?: string; status?: number }
  request?: { method?: string; url?: string }
}

/**
 * Wrapper for API calls that manages loading state and error handling.
 *
 * This function:
 * - Sets loading state synchronously before the API call
 * - Shows error toasts for failed requests
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

  const clientConfig: Config = {
    baseUrl:
      typeof window !== "undefined" && window.location.origin
        ? window.location.origin
        : "http://localhost:9081",
    credentials: "include",
    responseStyle: "fields",
    throwOnError: false,
  }

  // Configure global client with SDK response format
  globalClient.setConfig(clientConfig)

  // Add response interceptor to globalClient to handle 401 errors
  globalClient.interceptors.response.use((response, request) => {
    if (response.status === 401) {
      const method = request.method
      if (
        method === "GET" ||
        // eslint-disable-next-line no-alert
        window.confirm(
          "You are logged out. Do you want to log in (and lose the current changes)?"
        )
      ) {
        loginOrRegisterAndHaltThisThread()
      }
    }
    return response
  })

  // Configure non-reloading client with same settings (no 401 interceptor)
  nonReloadingClient.setConfig(clientConfig)
}

/**
 * Handles API errors from SDK result format.
 * Shows error toasts and handles special cases (401, 404, 400).
 * 401 and 404 errors are handled silently (no toast).
 */
function handleSdkError(result: SdkResult) {
  if (!apiStatusHandler) return
  if (!result.error) return

  const status = result.response?.status
  const errorBody = result.error

  if (status === 401) {
    return
  }

  if (status === 404) {
    return
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

  // Show error toast
  const toast = useToast()
  toast.error(msg, {
    timeout: 3000,
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
