import { MockResponseInit } from "vitest-fetch-mock";
import ApiMockExpectation from "./apiMockImpl/ApiMockExpectation";

interface ApiMockBuilder {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  andReturnOnce(value: any): ApiMockExpectation;
  andRespondOnceWith404(): void;
  andRespondOnce(response: MockResponseInit): void;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  andRespondWithPromiseResolve(resolve: () => any): void;
}

interface ApiMock {
  expectingGet(url: string): ApiMockBuilder;
  expectingPost(url: string): ApiMockBuilder;
  expectingPatch(url: string): ApiMockBuilder;
  verifyCall(url: string): void;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  verifyCall(url: string, matcher: any): void;
  assertNoUnexpectedOrMissedCalls(): void;
  close(): void;
}

export type { ApiMock, ApiMockBuilder };
