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

  api;

  constructor() {
    this.api = new Api("/api/");
  }

  // eslint-disable-next-line class-methods-use-this
  private around<T>(promise: Promise<T>): Promise<T> {
    const assignLoading = (value: boolean) => {
      if (value) {
        ManagedApi.statusWrap.apiStatus.states.push(true);
      } else {
        ManagedApi.statusWrap.apiStatus.states.pop();
      }
    };

    assignLoading(true);
    return new Promise((resolve) => {
      promise.then(resolve).finally(() => assignLoading(false));
    });
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
export type { ApiStatus };
