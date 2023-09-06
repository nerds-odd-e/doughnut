import { MockResponseInit } from "vitest-fetch-mock";
import ApiMockExpectation from "./apiMockImpl/ApiMockExpectation";

interface ApiMockBuilder {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  andReturnOnce(value: any): ApiMockExpectation;
  andRespondOnceWith404(): void;
  andRespondOnce(response: MockResponseInit): void;
  /*
   * This method is used to insert a callback that will be called
   * asynchronously when the request is made (using setTimeout(..., 0)).
   * This is useful when testing interium states of the application during
   * the request.
   *
   * Please don't do assertions in the callback, as they will be called
   * asynchronously and might not be caught by the test runner.
   * You can use it to collect data during the request and then assert
   * on the data after the request is finished.
   */
  andRespondWithAsyncPromiseResolve(
    resolve: (request: Request) => MockResponseInit | void,
  ): void;
}

interface ApiMock {
  expectingGet(url: string): ApiMockBuilder;
  expectingPost(url: string): ApiMockBuilder;
  expectingDelete(url: string): ApiMockBuilder;
  expectingPatch(url: string): ApiMockBuilder;
  verifyCall(url: string): void;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  verifyCall(url: string, matcher: any): void;
  assertNoUnexpectedOrMissedCalls(): void;
  close(): void;
}

export type { ApiMock, ApiMockBuilder };
