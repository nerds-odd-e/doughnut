import type { ApiRequestOptions } from "@generated/backend/core/ApiRequestOptions"
import type { CancelablePromise } from "@generated/backend/core/CancelablePromise"
import type { OpenAPIConfig } from "@generated/backend/core/OpenAPI"

export abstract class BaseHttpRequest {
  constructor(public readonly config: OpenAPIConfig) {}

  public abstract request<T>(
    options: ApiRequestOptions<T>
  ): CancelablePromise<T>
}
