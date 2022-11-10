import { MockParams } from "vitest-fetch-mock";
import { ApiMockBuilder } from "../ApiMock";
import ApiMockExpectation from "./ApiMockExpectation";

class ApiMockBuilderImpl implements ApiMockBuilder {
  expectation;

  constructor(expectation: ApiMockExpectation) {
    this.expectation = expectation;
  }

  andRespondOnceWith404(): void {
    this.andRespondOnce({ status: 404 });
  }

  andRespondOnce(response: MockParams): void {
    this.expectation.response = response;
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  andReturnOnce(value: any): void {
    this.expectation.value = value;
  }
}

export default ApiMockBuilderImpl;
