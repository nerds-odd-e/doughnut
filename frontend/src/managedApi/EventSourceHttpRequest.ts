import type { OpenAPIConfig } from "generated/backend"
import { BaseHttpRequest, CancelablePromise } from "generated/backend"
import type { ApiRequestOptions } from "generated/backend/core/ApiRequestOptions"
import { getQueryString } from "generated/backend/core/request"
import createEventSourceWithBody from "./createEventSourceWithBody"

// getUrl
// this function is copied from the generated code at
// frontend/src/generated/backend/core/request.ts
// It is not exported from the generated code, so it is copied here
const getUrl = (config: OpenAPIConfig, options: ApiRequestOptions): string => {
  const encoder = config.ENCODE_PATH || encodeURI

  const path = options.url
    .replace("{api-version}", config.VERSION)
    .replace(/{(.*?)}/g, (substring: string, group: string) => {
      if (Object.hasOwn(options.path ?? {}, group)) {
        return encoder(String(options.path?.[group]))
      }
      return substring
    })

  const url = `${config.BASE}${path}`
  if (options.query) {
    return `${url}${getQueryString(options.query)}`
  }
  return url
}

export default class EventSourceHttpRequestImpl extends BaseHttpRequest {
  // eslint-disable-next-line class-methods-use-this
  onMessage: (event: string, data: string) => void = () => undefined

  onError?: (error: unknown) => void = undefined

  public override request<T>(options: ApiRequestOptions): CancelablePromise<T> {
    return new CancelablePromise(async (resolve, reject, onCancel) => {
      try {
        const url = getUrl(this.config, options)
        if (!onCancel.isCancelled) {
          createEventSourceWithBody(
            url,
            options.body,
            this.onMessage,
            this.onError
          )
          resolve(undefined as unknown as T)
        }
      } catch (error) {
        reject(error)
      }
    })
  }
}
