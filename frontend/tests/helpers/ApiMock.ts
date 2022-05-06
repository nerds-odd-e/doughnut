import { MockParams } from "jest-fetch-mock";

interface ApiMockBuilder {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  andReturnOnce(value: any): void;
  andRespondOnceWith404(): void;
  andRespondOnce(response: MockParams): void;
}

interface ApiMock {
  expecting(url: string): ApiMockBuilder;
  verifyCall(url: string): void;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  verifyCall(url: string, matcher: any): void;
  assertNoUnexpectedCalls(): void;
}

export { ApiMock, ApiMockBuilder };
