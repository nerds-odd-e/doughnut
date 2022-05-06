import fetchMock from "jest-fetch-mock";
import { ApiMock } from "../ApiMock";
import ApiMockBuilderImpl, { ApiMockExpectation } from "./ApiMockBuilderImpl";

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
        return match.response || JSON.stringify(match.value);
      }

      this.unexpectedApiCalls.push(request.url);
      return JSON.stringify({});
    });
    return this;
  }

  assertNoUnexpectedCalls() {
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
    return this.expected.map((exp) => exp.url);
  }

  expecting(url: string) {
    const newLength = this.expected.push({ url, value: {} });
    return new ApiMockBuilderImpl(this.expected[newLength - 1]);
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
    teardown: () => mockedApi.assertNoUnexpectedCalls(),
  };
};

export default setupApiMock;
