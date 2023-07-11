import { MockParams } from "vitest-fetch-mock";
import { HttpMethod } from "../../../src/managedApi/window/RestfulFetch";

class ApiMockExpectation {
  url: string;

  response?: MockParams | string;

  resolve?: (request: Request) => MockParams | void;

  method: HttpMethod;

  actual?: Request;

  constructor(url: string, method: HttpMethod) {
    this.url = url;
    this.method = method;
  }

  getResponse(
    request: Request,
  ): MockParams | string | Promise<MockParams | string> {
    if (this.resolve) {
      return new Promise((resolve) => {
        setTimeout(() => {
          if (this.resolve) {
            resolve(this.resolve(request) ?? "{}");
          }
        }, 0);
      });
    }
    return this.response ?? "{}";
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
