import { MockParams } from "vitest-fetch-mock";

interface ApiMockBuilder {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  andReturnOnce(value: any): void;
  andRespondOnceWith404(): void;
  andRespondOnce(response: MockParams): void;
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
