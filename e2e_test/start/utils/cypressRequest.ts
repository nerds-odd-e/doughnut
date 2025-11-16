/// <reference types="cypress" />
// Custom request function for e2e tests that uses cy.request instead of fetch
// This replaces the generated request function via esbuild alias

import type { ApiRequestOptions } from '@generated/backend/core/ApiRequestOptions'
import type { OpenAPIConfig } from '@generated/backend/core/OpenAPI'
import { CancelablePromise } from '@generated/backend/core/CancelablePromise'
import { ApiError } from '@generated/backend/core/ApiError'
import type { ApiResult } from '@generated/backend/core/ApiResult'

// Re-export helper functions that services might need
export {
  getFormData,
  getQueryString,
  getHeaders,
  getRequestBody,
} from '@generated/backend/core/request'

const isString = (value: unknown): value is string => {
  return typeof value === 'string'
}

const isStringWithValue = (value: unknown): value is string => {
  return isString(value) && value !== ''
}

const base64 = (str: string): string => {
  try {
    return btoa(str)
  } catch {
    // @ts-ignore
    return Buffer.from(str).toString('base64')
  }
}

const getQueryString = (params: Record<string, unknown>): string => {
  const qs: string[] = []

  const append = (key: string, value: unknown) => {
    qs.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
  }

  const encodePair = (key: string, value: unknown) => {
    if (value === undefined || value === null) {
      return
    }

    if (value instanceof Date) {
      append(key, value.toISOString())
    } else if (Array.isArray(value)) {
      value.forEach((v) => encodePair(key, v))
    } else if (typeof value === 'object') {
      Object.entries(value).forEach(([k, v]) => encodePair(`${key}[${k}]`, v))
    } else {
      append(key, value)
    }
  }

  Object.entries(params).forEach(([key, value]) => encodePair(key, value))

  return qs.length ? `?${qs.join('&')}` : ''
}

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

type Resolver<T> = (options: ApiRequestOptions<T>) => Promise<T>

const resolve = async <T>(
  options: ApiRequestOptions<T>,
  resolver?: T | Resolver<T>
): Promise<T | undefined> => {
  if (typeof resolver === 'function') {
    return (resolver as Resolver<T>)(options)
  }
  return resolver
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
      (headers, [key, value]) => ({
        ...headers,
        [key]: String(value),
      }),
      {} as Record<string, string>
    )

  if (isStringWithValue(token)) {
    headers.Authorization = `Bearer ${token}`
  }

  if (isStringWithValue(username) && isStringWithValue(password)) {
    const credentials = base64(`${username}:${password}`)
    headers.Authorization = `Basic ${credentials}`
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

const getRequestBody = (options: ApiRequestOptions): unknown => {
  if (options.body !== undefined) {
    if (
      options.mediaType?.includes('application/json') ||
      options.mediaType?.includes('+json')
    ) {
      return JSON.stringify(options.body)
    } else if (
      typeof options.body === 'string' ||
      options.body instanceof FormData
    ) {
      return options.body
    } else {
      return JSON.stringify(options.body)
    }
  }
  return undefined
}

const catchErrorCodes = (
  options: ApiRequestOptions,
  result: ApiResult
): void => {
  const errors: Record<number, string> = {
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

  const error = errors[result.status]
  if (error) {
    throw new ApiError(options, result, error)
  }

  if (!result.ok) {
    const errorStatus = result.status ?? 'unknown'
    const errorStatusText = result.statusText ?? 'unknown'
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
      `Generic Error: status: ${errorStatus}; status text: ${errorStatusText}; body: ${errorBody}`
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
    if (onCancel.isCancelled) {
      return
    }

    // Use cy.then() to ensure we're in the Cypress command chain
    cy.then(async () => {
      try {
        const url = getUrl(config, options)
        const body = getRequestBody(options)
        const headers = await getHeaders(config, options)

        // Use cy.request instead of fetch
        cy.request({
          method: options.method,
          url,
          headers,
          body: body as any,
          failOnStatusCode: false, // We'll handle errors ourselves
        }).then(async (response) => {
          try {
            // Validate that response is a valid Cypress response object
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

            // Validate that response has a status property
            // This check is critical - cy.request() should always return an object with a status property
            if (
              !('status' in response) ||
              response.status === undefined ||
              response.status === null
            ) {
              // Log the actual response structure for debugging
              const responseKeys = response ? Object.keys(response) : []
              throw new ApiError(
                options,
                {
                  url,
                  ok: false,
                  status: 0,
                  statusText: 'Unknown Status',
                  body: undefined,
                },
                `Invalid response: response.status is ${response.status}. Response keys: ${responseKeys.join(', ')}. Response object: ${JSON.stringify(response)}`
              )
            }

            // Process response through interceptors
            // Create a Response-like object for interceptors
            const responseLike = {
              ok: response.status >= 200 && response.status < 300,
              status: response.status,
              statusText: response.statusText || '',
              headers: new Headers(response.headers as Record<string, string>),
              body: response.body,
            } as Response

            let processedResponse = responseLike
            for (const fn of config.interceptors.response._fns) {
              processedResponse = (await fn(processedResponse)) as Response
            }

            // Extract response body
            let responseBody: unknown
            if (response.status !== 204) {
              try {
                if (typeof response.body === 'string') {
                  try {
                    responseBody = JSON.parse(response.body)
                  } catch {
                    responseBody = response.body
                  }
                } else {
                  responseBody = response.body
                }
              } catch {
                responseBody = undefined
              }
            } else {
              responseBody = undefined
            }

            // Get response header if specified
            const responseHeader = options.responseHeader
              ? response.headers[options.responseHeader.toLowerCase()]
              : undefined

            // Apply response transformer if provided
            let transformedBody = responseHeader ?? responseBody
            if (
              options.responseTransformer &&
              response.status >= 200 &&
              response.status < 300
            ) {
              transformedBody = await options.responseTransformer(responseBody)
            }

            const result: ApiResult = {
              url,
              ok: response.status >= 200 && response.status < 300,
              status: response.status,
              statusText: response.statusText || '',
              body: transformedBody,
            }

            catchErrorCodes(options, result)

            resolve(result.body as T)
          } catch (error) {
            // Handle any errors from cy.request() or processing
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
