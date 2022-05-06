import { MockParams } from "jest-fetch-mock";

type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "ANY";

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
    return (
      this.url === request.url &&
      (this.method === "ANY" || this.method === request.method)
    );
  }
}

export default ApiMockExpectation;
