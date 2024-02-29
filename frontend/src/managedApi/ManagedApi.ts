import { DoughnutApi } from "@/generated/backend";
import BindingHttpRequest from "./BindingHttpRequest";
import ApiStatusHandler, { ApiError, ApiStatus } from "./ApiStatusHandler";

class ManagedApi extends DoughnutApi {
  apiStatus: ApiStatus;

  apiStatusHandler: ApiStatusHandler;

  silentApi: ManagedApi | undefined = undefined;

  constructor(apiStatus: ApiStatus, silent?: boolean) {
    super({ BASE: "" }, BindingHttpRequest(apiStatus, silent));
    this.apiStatus = apiStatus;
    this.apiStatusHandler = new ApiStatusHandler(apiStatus, silent);
  }

  get silent(): ManagedApi {
    if (!this.silentApi) {
      this.silentApi = new ManagedApi(this.apiStatus, true);
    }
    return this.silentApi;
  }

  logout() {
    return this.request.request({
      method: "POST",
      url: "logout",
    });
  }
}

export default ManagedApi;
export type { ApiStatus, ApiError };
