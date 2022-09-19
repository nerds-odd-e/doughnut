import Api from "./Api";
import { JsonData } from "./window/RestfulFetch";

type ApiStatus = { states: boolean[] };

class ManagedApi {
  static statusWrap = {
    apiStatus: { states: [] } as ApiStatus,
  };

  static registerStatus(apiStatus: ApiStatus) {
    ManagedApi.statusWrap.apiStatus = apiStatus;
  }

  static around<T>(promise: Promise<T>): Promise<T> {
    const assignLoading = (value: boolean) => {
      if (value) {
        ManagedApi.statusWrap.apiStatus.states.push(true);
      } else {
        ManagedApi.statusWrap.apiStatus.states.pop();
      }
    };

    assignLoading(true);
    return promise.finally(() => assignLoading(false));
  }

  api;

  constructor() {
    this.api = new Api("/api/");
  }

  restGet(url: string) {
    return ManagedApi.around(this.api.restGet(url));
  }

  restPost(url: string, data: JsonData) {
    return ManagedApi.around(this.api.restPost(url, data));
  }

  restPatch(url: string, data: JsonData) {
    return ManagedApi.around(this.api.restPatch(url, data));
  }

  restPostMultiplePartForm(url: string, data: JsonData) {
    return ManagedApi.around(this.api.restPostMultiplePartForm(url, data));
  }

  restPatchMultiplePartForm(url: string, data: JsonData) {
    return ManagedApi.around(this.api.restPatchMultiplePartForm(url, data));
  }

  restPostWithHtmlResponse(url: string, data: JsonData) {
    return ManagedApi.around(this.api.restPostWithHtmlResponse(url, data));
  }
}

export default ManagedApi;
export type { ApiStatus };
