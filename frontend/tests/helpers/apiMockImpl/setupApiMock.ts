import fetchMock from "jest-fetch-mock";
import { ApiMock } from "../ApiMock";
import ApiMockBuilderImpl from "./ApiMockBuilderImpl";
import ApiMockExpectation from "./ApiMockExpectation";

function rejectFromArray<T>(array: T[], predicate: (value: T) => unknown): T[] {
  const firstMatch = array.findIndex(predicate);
  if (firstMatch === -1) return array;
  return array.filter((_, index) => index !== firstMatch);
}

class ApiMockImpl implements ApiMock {
  fetchMock = fetchMock;

  private unexpectedApiCalls: string[] = [];

  private expected: ApiMockExpectation[] = [];

  private actualApiCalls: Request[] = [];

  init() {
    this.fetchMock.doMock(async (request: Request) => {
      const matched = this.unmatchedExpectations.find((exp) =>
        exp.matchExpectation(request)
      );
      this.actualApiCalls.push(request);
      if (matched) {
        return matched.response || JSON.stringify(matched.value);
      }

      // this.unexpectedApiCalls.push(request.url);
      return JSON.stringify({});
    });
    return this;
  }

  assertNoUnexpectedOrMissedCalls() {
    try {
      if (this.unexpectedApiCalls.length > 0) {
        throw new Error(
          `Unexpected API calls: ${this.unexpectedApiCalls.join(", ")}`
        );
      }
      if (this.unmatchedExpectations.length > 0) {
        throw new Error(
          `Expected but missed API calls: ${this.unmatchedExpectations
            .map((exp) => exp.url)
            .join(", ")}`
        );
      }
    } finally {
      this.unexpectedApiCalls = [];
      this.expected = [];
      this.actualApiCalls = [];
    }
  }

  private get unmatchedExpectations() {
    return this.remainingExpectations(this.actualApiCalls);
  }

  private remainingExpectations(calls: Request[]): ApiMockExpectation[] {
    if (calls.length === 0) return this.expected;

    return rejectFromArray(
      this.remainingExpectations(calls.slice(0, calls.length - 1)),
      (exp) => exp.matchExpectation(calls[calls.length - 1])
    );
  }

  expecting(url: string, method: "GET" | "POST" | "PUT" | "ANY" = "ANY") {
    const newLength = this.expected.push(new ApiMockExpectation(url, method));
    return new ApiMockBuilderImpl(this.expected[newLength - 1]);
  }

  expectingGet(url: string) {
    return this.expecting(url, "GET");
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  verifyCall(url: string, matcher: any = expect.anything()) {
    const unexpectedIndex = this.unexpectedApiCalls.indexOf(url);
    if (unexpectedIndex >= 0)
      this.unexpectedApiCalls.splice(unexpectedIndex, 1);

    expect(this.fetchMock).toHaveBeenCalledWith(url, matcher);
  }
}

const setupApiMock = () => new ApiMockImpl().init() as ApiMock;

export default setupApiMock;
