/// <reference types="cypress" />
// Custom request function for e2e tests that uses cy.request instead of fetch
// This replaces the generated request function via esbuild alias

import type { ApiRequestOptions } from '@generated/backend/core/ApiRequestOptions'
import type { OpenAPIConfig } from '@generated/backend/core/OpenAPI'
import { CancelablePromise } from '@generated/backend/core/CancelablePromise'
import { ApiError } from '@generated/backend/core/ApiError'
import type { ApiResult } from '@generated/backend/core/ApiResult'
import {
  getQueryString,
  getRequestBody as getRequestBodyFromGenerated,
  resolve,
  isStringWithValue,
  base64,
} from '@generated/backend/core/request'

// Re-export helper functions that services might need
export { getFormData, getRequestBody } from '@generated/backend/core/request'

const getUrl = (config: OpenAPIConfig, options: ApiRequestOptions): string => {
  const encoder = config.ENCODE_PATH || encodeURI
  const path = options.url
    .replace('{api-version}', config.VERSION)
    .replace(/{(.*?)}/g, (substring: string, group: string) => {
      if (options.path && Object.hasOwn(options.path, group)) {
        return encoder(String(options.path[group]))
      }
      return substring
    })
  const url = config.BASE + path
  return options.query ? url + getQueryString(options.query) : url
}

const getHeaders = async <T>(
  config: OpenAPIConfig,
  options: ApiRequestOptions<T>
): Promise<Record<string, string>> => {
  const [token, username, password, additionalHeaders] = await Promise.all([
    // @ts-ignore
    resolve(options, config.TOKEN),
    // @ts-ignore
    resolve(options, config.USERNAME),
    // @ts-ignore
    resolve(options, config.PASSWORD),
    // @ts-ignore
    resolve(options, config.HEADERS),
  ])

  const headers = Object.entries({
    Accept: 'application/json',
    ...additionalHeaders,
    ...options.headers,
  })
    .filter(([, value]) => value !== undefined && value !== null)
    .reduce(
      (acc, [key, value]) => ({ ...acc, [key]: String(value) }),
      {} as Record<string, string>
    )

  if (isStringWithValue(token)) {
    headers.Authorization = `Bearer ${token}`
  }

  if (isStringWithValue(username) && isStringWithValue(password)) {
    headers.Authorization = `Basic ${base64(`${username}:${password}`)}`
  }

  if (options.body !== undefined) {
    if (options.mediaType) {
      headers['Content-Type'] = options.mediaType
    } else if (typeof options.body === 'string') {
      headers['Content-Type'] = 'text/plain'
    } else if (!(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json'
    }
  }

  return headers
}

const getRequestBody = getRequestBodyFromGenerated

const validateResponse = (
  response: unknown,
  options: ApiRequestOptions,
  url: string
): Cypress.Response<unknown> => {
  if (!response || typeof response !== 'object') {
    throw new ApiError(
      options,
      {
        url,
        ok: false,
        status: 0,
        statusText: 'Invalid Response',
        body: undefined,
      },
      `Invalid response: response is not an object. Response: ${JSON.stringify(response)}`
    )
  }

  const responseObj = response as Record<string, unknown>
  if (
    !('status' in responseObj) ||
    responseObj.status === undefined ||
    responseObj.status === null
  ) {
    const responseKeys = Object.keys(responseObj)
    throw new ApiError(
      options,
      {
        url,
        ok: false,
        status: 0,
        statusText: 'Unknown Status',
        body: undefined,
      },
      `Invalid response: response.status is ${responseObj.status}. Response keys: ${responseKeys.join(', ')}. Response object: ${JSON.stringify(response)}`
    )
  }

  return response as Cypress.Response<unknown>
}

const parseResponseBody = (body: unknown, status: number): unknown => {
  if (status === 204) return undefined
  if (typeof body === 'string') {
    try {
      return JSON.parse(body)
    } catch {
      return body
    }
  }
  return body
}

const catchErrorCodes = (
  options: ApiRequestOptions,
  result: ApiResult
): void => {
  const errorMessages: Record<number, string> = {
    400: 'Bad Request',
    401: 'Unauthorized',
    402: 'Payment Required',
    403: 'Forbidden',
    404: 'Not Found',
    405: 'Method Not Allowed',
    406: 'Not Acceptable',
    407: 'Proxy Authentication Required',
    408: 'Request Timeout',
    409: 'Conflict',
    410: 'Gone',
    411: 'Length Required',
    412: 'Precondition Failed',
    413: 'Payload Too Large',
    414: 'URI Too Long',
    415: 'Unsupported Media Type',
    416: 'Range Not Satisfiable',
    417: 'Expectation Failed',
    418: 'Im a teapot',
    421: 'Misdirected Request',
    422: 'Unprocessable Content',
    423: 'Locked',
    424: 'Failed Dependency',
    425: 'Too Early',
    426: 'Upgrade Required',
    428: 'Precondition Required',
    429: 'Too Many Requests',
    431: 'Request Header Fields Too Large',
    451: 'Unavailable For Legal Reasons',
    500: 'Internal Server Error',
    501: 'Not Implemented',
    502: 'Bad Gateway',
    503: 'Service Unavailable',
    504: 'Gateway Timeout',
    505: 'HTTP Version Not Supported',
    506: 'Variant Also Negotiates',
    507: 'Insufficient Storage',
    508: 'Loop Detected',
    510: 'Not Extended',
    511: 'Network Authentication Required',
    ...options.errors,
  }

  const errorMessage = errorMessages[result.status]
  if (errorMessage) {
    throw new ApiError(options, result, errorMessage)
  }

  if (!result.ok) {
    const errorBody = (() => {
      try {
        return JSON.stringify(result.body, null, 2)
      } catch {
        return undefined
      }
    })()
    throw new ApiError(
      options,
      result,
      `Generic Error: status: ${result.status ?? 'unknown'}; status text: ${result.statusText ?? 'unknown'}; body: ${errorBody}`
    )
  }
}

/**
 * Request method for e2e tests using cy.request
 * @param config The OpenAPI configuration object
 * @param options The request options from the service
 * @returns CancelablePromise<T>
 * @throws ApiError
 */
export const request = <T>(
  config: OpenAPIConfig,
  options: ApiRequestOptions<T>
): CancelablePromise<T> => {
  return new CancelablePromise((resolve, reject, onCancel) => {
    if (onCancel.isCancelled) return

    cy.then(async () => {
      try {
        const url = getUrl(config, options)
        const body = getRequestBody(options)
        const headers = await getHeaders(config, options)

        cy.request({
          method: options.method,
          url,
          headers,
          body: body as any,
          failOnStatusCode: false,
        }).then(async (response) => {
          try {
            const validatedResponse = validateResponse(response, options, url)

            // Process response through interceptors
            const responseLike = {
              ok:
                validatedResponse.status >= 200 &&
                validatedResponse.status < 300,
              status: validatedResponse.status,
              statusText: validatedResponse.statusText || '',
              headers: new Headers(
                validatedResponse.headers as Record<string, string>
              ),
              body: validatedResponse.body,
            } as Response

            let processedResponse = responseLike
            for (const fn of config.interceptors.response._fns) {
              processedResponse = (await fn(processedResponse)) as Response
            }

            // Parse response body
            const responseBody = parseResponseBody(
              validatedResponse.body,
              validatedResponse.status
            )

            // Get response header if specified
            const responseHeader = options.responseHeader
              ? validatedResponse.headers[options.responseHeader.toLowerCase()]
              : undefined

            // Apply response transformer if provided
            let transformedBody = responseHeader ?? responseBody
            if (
              options.responseTransformer &&
              validatedResponse.status >= 200 &&
              validatedResponse.status < 300
            ) {
              transformedBody = await options.responseTransformer(responseBody)
            }

            const result: ApiResult = {
              url,
              ok:
                validatedResponse.status >= 200 &&
                validatedResponse.status < 300,
              status: validatedResponse.status,
              statusText: validatedResponse.statusText || '',
              body: transformedBody,
            }

            catchErrorCodes(options, result)
            resolve(result.body as T)
          } catch (error) {
            if (error instanceof ApiError) {
              reject(error)
            } else {
              const errorMessage =
                error && typeof error === 'object' && 'message' in error
                  ? String(error.message)
                  : String(error)
              reject(
                new ApiError(
                  options,
                  {
                    url,
                    ok: false,
                    status: 0,
                    statusText: 'Request Failed',
                    body: undefined,
                  },
                  `cy.request() failed: ${errorMessage}`
                )
              )
            }
          }
        })
      } catch (error) {
        reject(error)
      }
    })
  })
}
