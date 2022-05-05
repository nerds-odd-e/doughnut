import fetchMock, { MockParams } from "jest-fetch-mock";

interface ApiMockBuilder {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  andReturn(value: any): void;
}

interface ApiMock {
  expecting(url: string): ApiMockBuilder;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  expecting(url: string, value: any): void;
  expectingResponse(url: string, response: MockParams): void;
  verifyCall(url: string): void;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  verifyCall(url: string, matcher: any): void;
}

type ApiMockExpectation = {
    url: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    value: any;
    response?: MockParams;
    called: boolean;
  };

class ApiMockBuilderImpl implements ApiMockBuilder {

  expectation

  constructor(expectation: ApiMockExpectation) {
    this.expectation = expectation
  }

  andReturn(value: any): void {
    this.expectation.value = value
  }
}

class ApiMockImpl implements ApiMock {
  fetchMock = fetchMock;

  private unexpectedApiCalls: string[] = [];

  private expected: ApiMockExpectation[] = [];

  init() {
    this.fetchMock.doMock(async (request: Request) => {
      const matchedIndex = this.expected.findIndex(
        (exp) => exp.url === request.url
      );
      const match = this.expected.splice(matchedIndex, 1)[0];
      if (match) {
        match.called = true;
        return match.response || JSON.stringify(match.value);
      }

      this.unexpectedApiCalls.push(request.url);
      return JSON.stringify({});
    });
    return this;
  }

  noUnexpectedCalls() {
    if (this.unexpectedApiCalls.length > 0) {
      throw new Error(
        `Unexpected API calls: ${this.unexpectedApiCalls.join(", ")}`
      );
    }
    if (this.mismatchedApiCalls.length > 0) {
      throw new Error(
        `Expected but missed API calls: ${this.mismatchedApiCalls.join(", ")}`
      );
    }
  }

  private get mismatchedApiCalls(): string[] {
    return this.expected.filter((exp) => !exp.called).map((exp) => exp.url);
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  expecting(url: string, value: any = {}) {
    const newLength = this.expected.push({ url, value, called: false });
    return new ApiMockBuilderImpl(this.expected[newLength - 1]);
  }

  expectingResponse(url: string, response: MockParams) {
    this.expected.push({ url, value: {}, response, called: false });
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  verifyCall(url: string, matcher: any = expect.anything()) {
    const unexpectedIndex = this.unexpectedApiCalls.indexOf(url);
    if (unexpectedIndex >= 0)
      this.unexpectedApiCalls.splice(unexpectedIndex, 1);

    expect(this.fetchMock).toHaveBeenCalledWith(url, matcher);
  }
}

const setupApiMock = () => {
  const mockedApi = new ApiMockImpl().init();
  return {
    mockedApi: mockedApi as ApiMock,
    teardown: () => mockedApi.noUnexpectedCalls(),
  };
};

export { setupApiMock };

export default ApiMock;
