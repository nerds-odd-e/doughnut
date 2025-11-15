// eslint-disable-next-line max-classes-per-file
import { OpenAPI, ApiError, CancelablePromise } from "@generated/backend"
import { DoughnutApi } from "./DoughnutApi"
import type { ApiStatus } from "./ApiStatusHandler"
import ApiStatusHandler from "./ApiStatusHandler"
import assignBadRequestProperties from "./window/assignBadRequestProperties"
import loginOrRegisterAndHaltThisThread from "./window/loginOrRegisterAndHaltThisThread"
import EventSourceHttpRequestImpl from "./EventSourceHttpRequest"

// Set up OpenAPI config
OpenAPI.BASE = ""
OpenAPI.VERSION = "0"
OpenAPI.WITH_CREDENTIALS = false
OpenAPI.CREDENTIALS = "include"

class ManagedApi extends DoughnutApi {
  apiStatus: ApiStatus

  apiStatusHandler: ApiStatusHandler

  silentApi: ManagedApi | undefined = undefined

  eventSourceApi: EventSourceApi | undefined = undefined

  private isSilent: boolean

  constructor(apiStatus: ApiStatus, silent?: boolean) {
    super()
    this.apiStatus = apiStatus
    this.isSilent = silent ?? false
    this.apiStatusHandler = new ApiStatusHandler(apiStatus, silent)

    // Wrap all service instances to use wrapServiceCall for error handling and loading states
    this.wrapServiceInstances()
  }

  // Helper to wrap all service instance methods with error handling
  private wrapServiceInstances() {
    // Capture `this` for use in the Proxy handler
    const self = this
    // Cache wrapped functions to maintain reference equality for test mocking
    // biome-ignore lint/complexity/noBannedTypes: Need to cache arbitrary functions
    const wrappedFunctions = new WeakMap<Function, Function>()

    // Create a proxy wrapper for a service instance
    const wrapInstance = <T extends object>(instance: T): T => {
      return new Proxy(instance, {
        get(target, prop) {
          const value = Reflect.get(target, prop)
          if (typeof value === "function") {
            // Check if the function is a Vitest mock/spy - don't wrap it
            // Vitest mocks have a 'mock' property
            if ("mock" in value && typeof value.mock === "object") {
              return value
            }

            // Check if we've already wrapped this function
            let wrappedFn = wrappedFunctions.get(value)
            if (!wrappedFn) {
              // Create and cache the wrapped function
              wrappedFn = (...args: unknown[]) => {
                return self.wrapServiceCall(() => value.apply(target, args))
              }
              wrappedFunctions.set(value, wrappedFn)
            }
            return wrappedFn
          }
          return value
        },
        set(target, prop, value) {
          // When a property is set (e.g., a test spy), clear the cached wrapped function
          const oldValue = Reflect.get(target, prop)
          if (oldValue && typeof oldValue === "function") {
            wrappedFunctions.delete(oldValue)
          }
          return Reflect.set(target, prop, value)
        },
      })
    }

    // Wrap all service instances
    this.restNoteController = wrapInstance(this.restNoteController)
    this.restTextContentController = wrapInstance(
      this.restTextContentController
    )
    this.restNoteCreationController = wrapInstance(
      this.restNoteCreationController
    )
    this.restLinkController = wrapInstance(this.restLinkController)
    this.restUserController = wrapInstance(this.restUserController)
    this.restSearchController = wrapInstance(this.restSearchController)
    this.restNotebookController = wrapInstance(this.restNotebookController)
    this.restCircleController = wrapInstance(this.restCircleController)
    this.restConversationMessageController = wrapInstance(
      this.restConversationMessageController
    )
    this.restMemoryTrackerController = wrapInstance(
      this.restMemoryTrackerController
    )
    this.restRecallPromptController = wrapInstance(
      this.restRecallPromptController
    )
    this.restRecallsController = wrapInstance(this.restRecallsController)
    this.restPredefinedQuestionController = wrapInstance(
      this.restPredefinedQuestionController
    )
    this.restWikidataController = wrapInstance(this.restWikidataController)
    this.restAiController = wrapInstance(this.restAiController)
    this.restAiAudioController = wrapInstance(this.restAiAudioController)
    this.restAssessmentController = wrapInstance(this.restAssessmentController)
    this.restBazaarController = wrapInstance(this.restBazaarController)
    this.restCertificateController = wrapInstance(
      this.restCertificateController
    )
    this.restFailureReportController = wrapInstance(
      this.restFailureReportController
    )
    this.restFineTuningDataController = wrapInstance(
      this.restFineTuningDataController
    )
    this.restGlobalSettingsController = wrapInstance(
      this.restGlobalSettingsController
    )
    this.restHealthCheckController = wrapInstance(
      this.restHealthCheckController
    )
    this.restNotebookCertificateApprovalController = wrapInstance(
      this.restNotebookCertificateApprovalController
    )
    this.restSubscriptionController = wrapInstance(
      this.restSubscriptionController
    )
    this.restCurrentUserInfoController = wrapInstance(
      this.restCurrentUserInfoController
    )
    this.testabilityRestController = wrapInstance(
      this.testabilityRestController
    )
    this.restAssimilationController = wrapInstance(
      this.restAssimilationController
    )
    this.mcpNoteCreationController = wrapInstance(
      this.mcpNoteCreationController
    )
  }

  // Helper to wrap generated function calls with error handling and loading states
  // This is used by service instances to handle ApiError and manage loading states
  wrapServiceCall<T>(fn: () => CancelablePromise<T>): CancelablePromise<T> {
    return new CancelablePromise<T>(async (resolve, reject, onCancel) => {
      // Set loading state
      if (!this.isSilent) {
        this.apiStatusHandler.assignLoading(true)
      }

      try {
        const originalPromise = fn()
        onCancel(() => originalPromise.cancel())
        const result = await originalPromise

        // Clear loading state on success
        if (!this.isSilent) {
          this.apiStatusHandler.assignLoading(false)
        }

        resolve(result)
      } catch (error) {
        // Clear loading state on error
        if (!this.isSilent) {
          this.apiStatusHandler.assignLoading(false)
        }

        // ApiError is thrown by the generated code when the response is not OK
        if (error instanceof ApiError) {
          this.handleApiError(error)
        }
        reject(error)
      }
    })
  }

  private handleApiError(error: ApiError) {
    if (this.isSilent) return

    if (error.status === 401) {
      if (
        error.request.method === "GET" ||
        // eslint-disable-next-line no-alert
        window.confirm(
          "You are logged out. Do you want to log in (and lose the current changes)?"
        )
      ) {
        loginOrRegisterAndHaltThisThread()
        return
      }
    }

    let msg =
      error.body &&
      typeof error.body === "object" &&
      "message" in error.body &&
      typeof error.body.message === "string"
        ? error.body.message
        : error.message

    // For 404 errors, include endpoint details for better debugging
    if (error.status === 404) {
      const method = error.request.method || "UNKNOWN"
      const url = error.url || error.request.url || "UNKNOWN"
      const enhancedMsg = `[404 Not Found] ${method} ${url}\n\n${msg}`
      Object.defineProperty(error, "message", {
        value: enhancedMsg,
        writable: true,
        configurable: true,
      })
      msg = enhancedMsg
    }

    this.apiStatusHandler.addError(msg, error.status)

    if (error.status === 400) {
      const jsonResponse =
        typeof error.body === "string" ? JSON.parse(error.body) : error.body
      assignBadRequestProperties(error, jsonResponse)
    }
  }

  get silent(): ManagedApi {
    // for testability reasons, we need to be able to create a silent api object
    // and memoize it. So that the method can be mocked
    if (!this.silentApi) {
      this.silentApi = new ManagedApi(this.apiStatus, true)
    }
    return this.silentApi
  }

  get eventSource(): EventSourceApi {
    // for testability reasons, we need to be able to create a event source api object
    // and memoize it. So that the method can be mocked
    if (!this.eventSourceApi) {
      this.eventSourceApi = new EventSourceApi(this.apiStatus)
    }
    return this.eventSourceApi
  }

  async logout() {
    await fetch("/logout", {
      method: "POST",
    })
  }
}

class EventSourceApi extends ManagedApi {
  public readonly eventSourceRequest: EventSourceHttpRequestImpl

  constructor(apiStatus: ApiStatus) {
    super(apiStatus, false)
    // TODO: Implement EventSource support with interceptors
    this.eventSourceRequest = new EventSourceHttpRequestImpl({
      BASE: "",
      VERSION: "0",
      WITH_CREDENTIALS: false,
      CREDENTIALS: "include",
      interceptors: {
        request: OpenAPI.interceptors.request,
        response: OpenAPI.interceptors.response,
      },
    })
  }

  onMessage(callback: (event: string, data: string) => void) {
    this.eventSourceRequest.onMessage = callback
    return this
  }

  onError(callback: (error: unknown) => void) {
    this.eventSourceRequest.onError = callback
    return this
  }
}

export default ManagedApi
export type { ApiStatus, ApiError }
