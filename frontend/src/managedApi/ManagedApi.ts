import { BaseHttpRequest, DoughnutApi, OpenAPIConfig } from "@/generated/backend"
import ApiStatusHandler, { ApiError, ApiStatus } from "./ApiStatusHandler"
import BindingHttpRequest from "./BindingHttpRequest"
import EventSourceHttpRequest from "./EventSourceHttpRequest"

class ManagedApi extends DoughnutApi {
  apiStatus: ApiStatus

  apiStatusHandler: ApiStatusHandler

  silentApi: ManagedApi | undefined = undefined

  constructor(apiStatus: ApiStatus, silent?: boolean, httpRequestConstructor?: new (config: OpenAPIConfig) => BaseHttpRequest) {
    super({ BASE: "" }, httpRequestConstructor ?? BindingHttpRequest(apiStatus, silent))
    this.apiStatus = apiStatus
    this.apiStatusHandler = new ApiStatusHandler(apiStatus, silent)
  }

  get silent(): ManagedApi {
    if (!this.silentApi) {
      this.silentApi = new ManagedApi(this.apiStatus, true)
    }
    return this.silentApi
  }

  eventSource(
    onMessage: (event: string, data: string) => void, onError?: (error: unknown) => void
  ): ManagedApi {
      return new ManagedApi(this.apiStatus, false, EventSourceHttpRequest(onMessage, onError))
  }

  logout() {
    return this.request.request({
      method: "POST",
      url: "logout",
    })
  }
}

export default ManagedApi
export type { ApiStatus, ApiError }
