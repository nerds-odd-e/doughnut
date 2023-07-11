import Api from "./Api";
import { JsonData } from "./window/RestfulFetch";

type ApiError = {
  id: number;
  message: string;
};

type ApiStatus = {
  states: boolean[];
  errors: ApiError[];
};

class ManagedApi {
  apiStatus: ApiStatus;

  api: Api;

  private silentMode?: boolean;

  constructor(apiStatus: ApiStatus, silent?: boolean) {
    this.apiStatus = apiStatus;
    this.api = new Api("/api/");
    this.silentMode = silent;
  }

  get silent(): ManagedApi {
    return new ManagedApi(this.apiStatus, true);
  }

  private assignLoading(value: boolean) {
    if (this.silentMode) return;
    if (value) {
      this.apiStatus.states.push(true);
    } else {
      this.apiStatus.states.pop();
    }
  }

  private addError(message: string): void {
    const id = Date.now();
    this.apiStatus.errors.push({ message, id });
    setTimeout(() => {
      this.apiStatus.errors = this.apiStatus.errors.filter(
        (error) => error.id !== id,
      );
    }, 2000);
  }

  async around<T>(promise: Promise<T>): Promise<T> {
    this.assignLoading(true);
    try {
      try {
        return await promise;
      } catch (error) {
        if (error instanceof Error) {
          this.addError(error.message);
        }
        throw error;
      }
    } finally {
      this.assignLoading(false);
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
