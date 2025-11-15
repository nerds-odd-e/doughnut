// eslint-disable-next-line max-classes-per-file
import type { OpenAPIConfig, BaseHttpRequest } from "@generated/backend"
import { DoughnutApi } from "./DoughnutApi"
import type { ApiError, ApiStatus } from "./ApiStatusHandler"
import ApiStatusHandler from "./ApiStatusHandler"
import BindingHttpRequest from "./BindingHttpRequest"
import EventSourceHttpRequest from "./EventSourceHttpRequest"

class ManagedApi extends DoughnutApi {
  apiStatus: ApiStatus

  apiStatusHandler: ApiStatusHandler

  silentApi: ManagedApi | undefined = undefined

  eventSourceApi: EventSourceApi | undefined = undefined

  constructor(
    apiStatus: ApiStatus,
    silent?: boolean,
    httpRequestConstructor?: new (config: OpenAPIConfig) => BaseHttpRequest
  ) {
    super(
      { BASE: "" },
      httpRequestConstructor ?? BindingHttpRequest(apiStatus, silent)
    )
    this.apiStatus = apiStatus
    this.apiStatusHandler = new ApiStatusHandler(apiStatus, silent)
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

  logout() {
    return this.request.request({
      method: "POST",
      url: "/logout",
    })
  }
}

class EventSourceApi extends ManagedApi {
  public readonly eventSourceRequest: EventSourceHttpRequest

  constructor(apiStatus: ApiStatus) {
    super(apiStatus, false, EventSourceHttpRequest)
    this.eventSourceRequest = this.request as EventSourceHttpRequest
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
