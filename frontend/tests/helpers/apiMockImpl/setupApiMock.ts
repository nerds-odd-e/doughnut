import { HttpMethod } from "@/managedApi/window/RestfulFetch";
import { ApiMock } from "../ApiMock";
import ApiMockBuilderImpl from "./ApiMockBuilderImpl";
import ApiMockExpectation from "./ApiMockExpectation";

function rejectFromArray<T>(array: T[], predicate: (value: T) => unknown): T[] {
  const firstMatch = array.findIndex(predicate);
  if (firstMatch === -1) return array;
  return array.filter((_, index) => index !== firstMatch);
}

function requestDescription(request: Request): string {
  return `${request.method} ${request.url}`;
}

class ApiMockImpl implements ApiMock {
  fetchMock = fetchMock;

  private expected: ApiMockExpectation[] = [];

  private actualApiCalls: Request[] = [];

  private previousError?: Error;

  init() {
    this.fetchMock.doMock(async (request: Request) => {
      const matched = this.unmatchedExpectations.find((exp) =>
        exp.matchExpectation(request)
      );
      this.actualApiCalls.push(request);
      if (matched) {
        return matched.response ?? "{}";
      }
      this.previousError = new Error(
        `Unexpected API call: '${requestDescription(request)}'`
      );
      throw this.previousError;
    });
    return this;
  }

  close() {
    this.fetchMock.resetMocks();
  }

  assertNoUnexpectedOrMissedCalls() {
    try {
      if (this.previousError) {
        this.previousError.message = `This error happened in an async api call (perhaps it's not expected by apiMock).\n If you cannot find the same error above, perhaps the production code have swallowed it.\n The original message: ${this.previousError.message}`;
        throw this.previousError;
      }
      if (this.unmatchedExpectations.length > 0) {
        throw new Error(
          `Expected but missed API calls: ${this.unmatchedExpectations
            .map((exp) => exp.url)
            .join(", ")}`
        );
      }
    } finally {
      this.previousError = undefined;
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
      (exp) => exp.matchExpectation(calls[calls.length - 1] as Request)
    );
  }

  private expecting(url: string, method: HttpMethod) {
    const newLength = this.expected.push(new ApiMockExpectation(url, method));
    return new ApiMockBuilderImpl(
      this.expected[newLength - 1] as ApiMockExpectation
    );
  }

  expectingGet(url: string) {
    return this.expecting(url, "GET");
  }

  expectingPatch(url: string) {
    return this.expecting(url, "PATCH");
  }

  expectingPost(url: string) {
    return this.expecting(url, "POST");
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  verifyCall(url: string, matcher: any = expect.anything()) {
    expect(this.fetchMock).toHaveBeenCalledWith(url, matcher);
  }
}

const setupApiMock = () => new ApiMockImpl().init() as ApiMock;

export default setupApiMock;
