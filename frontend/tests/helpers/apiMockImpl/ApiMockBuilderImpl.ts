import { MockResponseInit } from "vitest-fetch-mock";
import { ApiMockBuilder } from "../ApiMock";
import ApiMockExpectation from "./ApiMockExpectation";

class ApiMockBuilderImpl implements ApiMockBuilder {
  expectation: ApiMockExpectation;

  constructor(expectation: ApiMockExpectation) {
    this.expectation = expectation;
  }

  andRespondOnceWith404(): void {
    this.andRespondOnce({ status: 404 });
  }

  andRespondOnce(response: MockResponseInit): void {
    this.expectation.response = response;
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  andReturnOnce(value: any): ApiMockExpectation {
    this.expectation.response = JSON.stringify(value);
    return this.expectation;
  }

  andRespondWithAsyncPromiseResolve(
    resolve: (request: Request) => MockResponseInit | void,
  ): ApiMockExpectation {
    this.expectation.resolve = resolve;
    return this.expectation;
  }
}

export default ApiMockBuilderImpl;
