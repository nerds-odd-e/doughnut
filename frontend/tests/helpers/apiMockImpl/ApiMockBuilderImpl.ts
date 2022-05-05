import { MockParams } from "jest-fetch-mock";
import { ApiMockBuilder } from "../ApiMock";

type ApiMockExpectation = {
  url: string;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  value: any;
  response?: MockParams;
  called: boolean;
};

class ApiMockBuilderImpl implements ApiMockBuilder {
  expectation;

  constructor(expectation: ApiMockExpectation) {
    this.expectation = expectation;
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  andReturn(value: any): void {
    this.expectation.value = value;
  }
}

export default ApiMockBuilderImpl;
export { ApiMockExpectation };
