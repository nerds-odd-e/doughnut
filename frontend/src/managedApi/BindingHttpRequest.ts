import { ApiError, CancelablePromise } from "@/generated/backend";
import { ApiRequestOptions } from "@/generated/backend/core/ApiRequestOptions";
import { FetchHttpRequest } from "@/generated/backend/core/FetchHttpRequest";
import loginOrRegisterAndHaltThisThread from "./window/loginOrRegisterAndHaltThisThread";

export default class BindingHttpRequest extends FetchHttpRequest {
  public override request<T>(options: ApiRequestOptions): CancelablePromise<T> {
    return new CancelablePromise<T>((resolve, reject, onCancel) => {
      const originalPromise = super.request<T>(options);

      onCancel(() => originalPromise.cancel());

      originalPromise.then(resolve).catch((error: unknown) => {
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
}
