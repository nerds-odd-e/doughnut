import { client as globalClient } from "@generated/doughnut-backend-api/client.gen"
import {
  createClient,
  type Config,
} from "@generated/doughnut-backend-api/client"
import type {
  ApiLoadingOptions,
  ApiLoadingState,
  ApiStatus,
} from "./ApiStatusHandler"
import ApiStatusHandler from "./ApiStatusHandler"
import assignBadRequestProperties from "./window/assignBadRequestProperties"
import loginOrRegisterAndHaltThisThread from "./window/loginOrRegisterAndHaltThisThread"
import { useToast } from "vue-toastification"

function apiRequestLabel(request: Request): string {
  const method = request.method?.toUpperCase() ?? "?"
  try {
    const u = new URL(request.url)
    return `${method} ${u.pathname}${u.search}`
  } catch {
    return `${method} ${request.url}`
  }
}

let apiStatusHandler: ApiStatusHandler | undefined

export const nonReloadingClient = createClient()

type SdkResult = {
  data?: unknown
  error?: unknown
  response?: { url?: string; status?: number }
  request?: { method?: string; url?: string }
}

type CompositeSdkResult<T> = SdkResult & {
  data: T
}

export type CancelableApiLoadingOptions = ApiLoadingOptions & {
  blockUi: true
  cancelable: true
}

type NonCancelableApiLoadingOptions = ApiLoadingOptions & { cancelable?: never }

export type CancelableApiResult<T> =
  | { status: "completed"; result: T }
  | { status: "cancelled" }

export function apiCallWithLoading<T extends SdkResult>(
  apiCall: (signal: AbortSignal) => Promise<T>,
  options: CancelableApiLoadingOptions
): Promise<CancelableApiResult<T>>
export function apiCallWithLoading<T extends SdkResult>(
  apiCall: () => Promise<T>,
  options?: NonCancelableApiLoadingOptions
): Promise<T>
export async function apiCallWithLoading<T extends SdkResult>(
  apiCall: (() => Promise<T>) | ((signal: AbortSignal) => Promise<T>),
  options: ApiLoadingOptions | CancelableApiLoadingOptions = {}
): Promise<T | CancelableApiResult<T>> {
  const statusHandler = apiStatusHandler
  if ("cancelable" in options && options.cancelable) {
    const controller = new AbortController()
    const cancelableCall = apiCall as (signal: AbortSignal) => Promise<T>
    if (!statusHandler) {
      return {
        status: "completed",
        result: await cancelableCall(controller.signal),
      }
    }

    const cancelled = { status: "cancelled" } as const
    let cancellationAccepted = false
    let loadingState: ApiLoadingState | undefined
    let resolveCancellation: (result: typeof cancelled) => void = () =>
      undefined
    const cancellation = new Promise<typeof cancelled>((resolve) => {
      resolveCancellation = resolve
    })
    const acceptCancellation = () => {
      if (
        cancellationAccepted ||
        !loadingState ||
        !statusHandler.finishLoading(loadingState)
      ) {
        return
      }
      cancellationAccepted = true
      controller.abort()
      resolveCancellation(cancelled)
    }

    loadingState = statusHandler.startLoading(options, acceptCancellation)
    let operation: Promise<T>
    try {
      operation = cancelableCall(controller.signal)
    } catch (error) {
      statusHandler.finishLoading(loadingState)
      throw error
    }
    const observedOperation = operation
      .then(
        (result): CancelableApiResult<T> => {
          if (cancellationAccepted) return cancelled
          if (result.error) handleSdkError(result)
          return { status: "completed", result }
        },
        (error): CancelableApiResult<T> => {
          if (cancellationAccepted) return cancelled
          throw error
        }
      )
      .finally(() => statusHandler.finishLoading(loadingState))

    return await Promise.race([observedOperation, cancellation])
  }

  if (!statusHandler) {
    return await (apiCall as () => Promise<T>)()
  }

  const loadingState = statusHandler.startLoading(options)
  try {
    const result = await (apiCall as () => Promise<T>)()

    if (result.error) {
      handleSdkError(result)
    }

    return result
  } finally {
    statusHandler.finishLoading(loadingState)
  }
}

export async function runWithBlockingApiLoading<T>(
  operation: () => Promise<T>,
  message: string
): Promise<T> {
  const { data } = await apiCallWithLoading<CompositeSdkResult<T>>(
    async () => ({ data: await operation() }),
    { blockUi: true, message }
  )
  return data
}

export function teardownGlobalClientForTesting() {
  apiStatusHandler = undefined
}

export function setupGlobalClient(apiStatus: ApiStatus) {
  apiStatusHandler = new ApiStatusHandler(apiStatus)

  const clientConfig: Config = {
    baseUrl:
      typeof window !== "undefined" && window.location.origin
        ? window.location.origin
        : "http://localhost:9081",
    credentials: "include",
    responseStyle: "fields",
    throwOnError: false,
  }

  globalClient.setConfig(clientConfig)

  globalClient.interceptors.response.use((response, request) => {
    if (response.status === 401) {
      const method = request.method
      const willRedirect =
        method === "GET" ||
        // eslint-disable-next-line no-alert
        window.confirm(
          "You are logged out. Do you want to log in (and lose the current changes)?"
        )
      if (willRedirect) {
        const apiName = apiRequestLabel(request)
        try {
          useToast().warning(
            `This page will reload to sign you in again. Reason: unauthorized response from ${apiName}.`,
            { timeout: 8000, pauseOnFocusLoss: true, pauseOnHover: true }
          )
        } catch {
          // useToast() needs component context; interceptors run outside setup()
        }
        loginOrRegisterAndHaltThisThread()
      }
    }
    return response
  })

  nonReloadingClient.setConfig(clientConfig)
}

function handleSdkError(result: SdkResult) {
  if (!apiStatusHandler) return
  if (!result.error) return

  const status = result.response?.status
  const errorBody = result.error

  if (status === 401) {
    return
  }

  if (status === 404) {
    return
  }

  if (
    status === 409 &&
    errorBody &&
    typeof errorBody === "object" &&
    "errorType" in errorBody &&
    ((errorBody as { errorType?: string }).errorType ===
      "SOFT_DELETED_TITLE_CONFLICT" ||
      (errorBody as { errorType?: string }).errorType ===
        "FOLDER_NAME_CONFLICT")
  ) {
    return
  }

  let msg = "API Error"
  if (
    errorBody &&
    typeof errorBody === "object" &&
    "message" in errorBody &&
    typeof errorBody.message === "string"
  ) {
    msg = errorBody.message
  } else if (typeof errorBody === "string") {
    msg = errorBody
  }

  const toast = useToast()
  toast.error(msg, {
    timeout: 3000,
    pauseOnFocusLoss: true,
    pauseOnHover: true,
  })

  if (status === 400 && errorBody) {
    const jsonResponse =
      typeof errorBody === "string" ? JSON.parse(errorBody) : errorBody
    assignBadRequestProperties(errorBody as Error, jsonResponse)
  }
}
