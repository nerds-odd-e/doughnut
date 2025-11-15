import { ApiError, CancelablePromise } from "@generated/backend"
import type { ApiRequestOptions } from "@generated/backend/core/ApiRequestOptions"
import { FetchHttpRequest } from "./FetchHttpRequest"
import type { ApiStatus } from "./ApiStatusHandler"
import ApiStatusHandler from "./ApiStatusHandler"
import assignBadRequestProperties from "./window/assignBadRequestProperties"
import loginOrRegisterAndHaltThisThread from "./window/loginOrRegisterAndHaltThisThread"

export default function BindingHttpRequest(
  apiStatus: ApiStatus,
  silent?: boolean
) {
  const apiStatusHandler = new ApiStatusHandler(apiStatus, silent)
  return class BindingHttpRequestWithStatus extends FetchHttpRequest {
    public override request<T>(
      options: ApiRequestOptions<T>
    ): CancelablePromise<T> {
      return new CancelablePromise<T>((resolve, reject, onCancel) => {
        const originalPromise = super.request<T>(options)

        onCancel(() => originalPromise.cancel())

        this.around(originalPromise)
          .then(resolve)
          .catch((error: unknown) => {
            if (error instanceof ApiError) {
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
                // Enhance the error message itself so Cypress can see it
                Object.defineProperty(error, "message", {
                  value: enhancedMsg,
                  writable: true,
                  configurable: true,
                })
                msg = enhancedMsg
              }

              apiStatusHandler.addError(msg, error.status)

              if (error.status === 400) {
                const jsonResponse =
                  typeof error.body === "string"
                    ? JSON.parse(error.body)
                    : error.body
                assignBadRequestProperties(error, jsonResponse)
              }
            }
            reject(error)
          })
      })
    }

    // eslint-disable-next-line class-methods-use-this
    private async around<T>(promise: Promise<T>): Promise<T> {
      apiStatusHandler.assignLoading(true)
      try {
        return await promise
      } finally {
        apiStatusHandler.assignLoading(false)
      }
    }
  }
}
