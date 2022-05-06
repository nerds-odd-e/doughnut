import { MockParams } from "jest-fetch-mock";

class ApiMockExpectation {
  url: string;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  value: any;

  response?: MockParams;

  method: "GET" | "POST" | "PUT" | "ANY";

  constructor(url: string, method: "GET" | "POST" | "PUT" | "ANY") {
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
