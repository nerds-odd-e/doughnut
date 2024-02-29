import { DoughnutApi } from "@/generated/backend";
import BindingHttpRequest from "./BindingHttpRequest";
import ApiStatusHandler, { ApiError, ApiStatus } from "./ApiStatusHandler";

class ManagedApi extends DoughnutApi {
  apiStatus: ApiStatus;

  apiStatusHandler: ApiStatusHandler;

  constructor(apiStatus: ApiStatus, silent?: boolean) {
    super({ BASE: "" }, BindingHttpRequest(apiStatus, silent));
    this.apiStatus = apiStatus;
    this.apiStatusHandler = new ApiStatusHandler(apiStatus, silent);
  }

  get silent(): ManagedApi {
    return new ManagedApi(this.apiStatus, true);
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
