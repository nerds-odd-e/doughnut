import { DoughnutApi } from "@/generated/backend";
import Api from "./Api";
import { JsonData } from "./window/RestfulFetch";
import BindingHttpRequest from "./BindingHttpRequest";
import ApiStatusHandler, { ApiError, ApiStatus } from "./ApiStatusHandler";

class ManagedApi extends DoughnutApi {
  apiStatus: ApiStatus;

  apiStatusHandler: ApiStatusHandler;

  api: Api;

  constructor(apiStatus: ApiStatus, silent?: boolean) {
    super({ BASE: "" }, BindingHttpRequest(apiStatus, silent));
    this.apiStatus = apiStatus;
    this.apiStatusHandler = new ApiStatusHandler(apiStatus, silent);
    this.api = new Api("/api/");
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

  async around<T>(promise: Promise<T>): Promise<T> {
    this.apiStatusHandler.assignLoading(true);
    try {
      try {
        return await promise;
      } catch (error) {
        if (error instanceof Error) {
          this.apiStatusHandler.addError(error.message);
        }
        throw error;
      }
    } finally {
      this.apiStatusHandler.assignLoading(false);
    }
  }

  restGet(url: string) {
    return this.around(this.api.restGet(url));
  }

  restPost(url: string, data: JsonData) {
    return this.around(this.api.restPost(url, data));
  }

  restPatch(url: string, data: JsonData) {
    return this.around(this.api.restPatch(url, data));
  }

  restDelete(url: string, data: JsonData) {
    return this.around(this.api.restDelete(url, data));
  }

  restPostMultiplePartForm(url: string, data: JsonData) {
    return this.around(this.api.restPostMultiplePartForm(url, data));
  }

  restPatchMultiplePartForm(url: string, data: JsonData) {
    return this.around(this.api.restPatchMultiplePartForm(url, data));
  }

  restPostWithHtmlResponse(url: string, data: JsonData) {
    return this.around(this.api.restPostWithHtmlResponse(url, data));
  }
}

export default ManagedApi;
export type { ApiStatus, ApiError };
