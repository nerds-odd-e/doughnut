import { MockParams } from "vitest-fetch-mock";
import { HttpMethod } from "../../../src/managedApi/window/RestfulFetch";

class ApiMockExpectation {
  url: string;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  value: any;

  response?: MockParams;

  method: HttpMethod;

  constructor(url: string, method: HttpMethod) {
    this.url = url;
    this.value = {};
    this.method = method;
  }

  matchExpectation(request: Request) {
    return this.url === request.url && this.method === request.method;
  }
}

export default ApiMockExpectation;
