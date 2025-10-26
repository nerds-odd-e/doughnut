import type { BaseHttpRequest } from '@generated/backend/core/BaseHttpRequest'
import type { ApiRequestOptions } from '@generated/backend/core/ApiRequestOptions'

// Create a capturing HTTP request to extract the request configuration from the generated service
export const extractRequestConfig = (
  serviceMethod: (httpRequest: BaseHttpRequest) => any
): ApiRequestOptions => {
  let capturedConfig: ApiRequestOptions | null = null

  const capturingHttpRequest: BaseHttpRequest = {
    request: (config: ApiRequestOptions) => {
      capturedConfig = config
      return Promise.resolve() as any
    },
  } as BaseHttpRequest

  serviceMethod(capturingHttpRequest)

  if (!capturedConfig) {
    throw new Error('Failed to extract request configuration')
  }

  return capturedConfig
}
