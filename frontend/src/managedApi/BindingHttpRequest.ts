import { ApiError, CancelablePromise } from "@/generated/backend";
import { ApiRequestOptions } from "@/generated/backend/core/ApiRequestOptions";
import { FetchHttpRequest } from "@/generated/backend/core/FetchHttpRequest";
import loginOrRegisterAndHaltThisThread from "./window/loginOrRegisterAndHaltThisThread";
import ApiStatusHandler, { ApiStatus } from "./ApiStatusHandler";

export default function BindingHttpRequest(
  apiStatus: ApiStatus,
  silent?: boolean,
) {
  const apiStatusHandler = new ApiStatusHandler(apiStatus, silent);
  return class BindingHttpRequestWithStatus extends FetchHttpRequest {
    public override request<T>(
      options: ApiRequestOptions,
    ): CancelablePromise<T> {
      return new CancelablePromise<T>((resolve, reject, onCancel) => {
        const originalPromise = super.request<T>(options);

        onCancel(() => originalPromise.cancel());

        this.around(originalPromise)
          .then(resolve)
          .catch((error: unknown) => {
            if (error instanceof Error) {
              apiStatusHandler.addError(error.message);
            }
            if (error instanceof ApiError && error.status === 401) {
              if (
                error.request.method === "GET" ||
                // eslint-disable-next-line no-alert
                window.confirm(
                  "You are logged out. Do you want to log in (and lose the current changes)?",
                )
              ) {
                loginOrRegisterAndHaltThisThread();
                return;
              }
            }
            reject(error);
          });
      });
    }

    // eslint-disable-next-line class-methods-use-this
    private async around<T>(promise: Promise<T>): Promise<T> {
      apiStatusHandler.assignLoading(true);
      try {
        return await promise;
      } finally {
        apiStatusHandler.assignLoading(false);
      }
    }
  };
}
