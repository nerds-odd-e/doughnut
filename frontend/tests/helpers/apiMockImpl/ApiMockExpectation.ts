import { MockParams } from "vitest-fetch-mock";
import { HttpMethod } from "../../../src/managedApi/window/RestfulFetch";

class ApiMockExpectation {
  url: string;

  response?: MockParams | string;

  method: HttpMethod;

  actual?: Request;

  constructor(url: string, method: HttpMethod) {
    this.url = url;
    this.method = method;
  }

  matchExpectation(request: Request): boolean {
    if (this.url !== request.url || this.method !== request.method) {
      return false;
    }
    this.actual = request;
    return true;
  }

  actualRequest(): Request {
    if (!this.actual) {
      throw new Error("Expectation never matched");
    }
    return this.actual;
  }

  actualRequestJsonBody(): unknown {
    return JSON.parse(this.actualRequest().body as unknown as string);
  }
}

export default ApiMockExpectation;
