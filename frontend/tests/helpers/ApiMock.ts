import { MockParams } from "jest-fetch-mock";

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

export { ApiMock, ApiMockBuilder };
