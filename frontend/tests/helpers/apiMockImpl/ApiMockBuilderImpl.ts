import { MockParams } from "jest-fetch-mock";
import { ApiMockBuilder } from "../ApiMock";

type ApiMockExpectation = {
  url: string;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  value: any;
  response?: MockParams;
};

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
export { ApiMockExpectation };
