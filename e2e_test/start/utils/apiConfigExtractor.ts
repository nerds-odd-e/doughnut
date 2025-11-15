import type { ApiRequestOptions } from '@generated/backend/core/ApiRequestOptions'
import type { OpenAPIConfig } from '@generated/backend/core/OpenAPI'
import type { CancelablePromise } from '@generated/backend/core/CancelablePromise'
import { DoughnutApi } from './DoughnutApiWrapper'

// Minimal BaseHttpRequest implementation for capturing request config
abstract class BaseHttpRequest {
  constructor(public readonly config: OpenAPIConfig) {}
  public abstract request<T>(options: ApiRequestOptions): CancelablePromise<T>
}

// Create a capturing HTTP request to extract the request configuration from DoughnutApi
export const extractRequestConfig = (
  serviceMethod: (api: DoughnutApi) => any
): ApiRequestOptions => {
  let capturedConfig: ApiRequestOptions | null = null

  class CapturingHttpRequest extends BaseHttpRequest {
    public override request(config: ApiRequestOptions): any {
      capturedConfig = config
      return Promise.resolve() as any
    }
  }

  const api = new DoughnutApi({ BASE: '' }, CapturingHttpRequest as any)
  serviceMethod(api)

  if (!capturedConfig) {
    throw new Error('Failed to extract request configuration')
  }

  return capturedConfig
}
