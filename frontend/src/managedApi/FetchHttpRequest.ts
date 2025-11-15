import type { ApiRequestOptions } from "@generated/backend/core/ApiRequestOptions"
import { BaseHttpRequest } from "./BaseHttpRequest"
import type { CancelablePromise } from "@generated/backend/core/CancelablePromise"
import { request as __request } from "@generated/backend/core/request"

export class FetchHttpRequest extends BaseHttpRequest {
  /**
   * Request method
   * @param options The request options from the service
   * @returns CancelablePromise<T>
   * @throws ApiError
   */
  public override request<T>(
    options: ApiRequestOptions<T>
  ): CancelablePromise<T> {
    return __request<T>(this.config, options)
  }
}
