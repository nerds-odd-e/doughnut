import { ApiError, CancelablePromise } from "@generated/backend"
import type { ApiRequestOptions } from "@generated/backend/core/ApiRequestOptions"
import { FetchHttpRequest } from "@generated/backend/core/FetchHttpRequest"
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
      options: ApiRequestOptions
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

              const msg = error.body ? error.body.message : error.message
              apiStatusHandler.addError(msg)

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
