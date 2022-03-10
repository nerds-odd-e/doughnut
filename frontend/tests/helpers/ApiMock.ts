import { FetchMock, MockParams } from "jest-fetch-mock";

class ApiMock {
  private fetchMock
  private unexpectedApiCalls: string[] = []
  private expected: {url: string, called: boolean}[] = []

  constructor(fetchMock: FetchMock) {
    this.fetchMock = fetchMock
    this.fetchMock.doMock(async (request: Request)=>{
      this.unexpectedApiCalls.push(request.url)
      // use empty json as default return value,
      // hopefully, the execution will be back to the
      // main thread and the exception makes more sense.
      return JSON.stringify({})
    })
  }

  noUnexpectedCalls() {
    expect(this.unexpectedApiCalls).toHaveLength(0)
    expect(this.mismatchedApiCalls).toHaveLength(0)
  }

  private get mismatchedApiCalls(): string[] {
    return this.expected.filter(exp => !exp.called).map(exp => exp.url);
  }

  mockJson(url: string, value: any={}) {
    const val = { url, called: false }
    this.expected.push(val)
    this.fetchMock.mockIf(url, async (request: Request)=>{
      val.called = true
      return JSON.stringify(value)
    })
  }

  mockResponse(url: string, response: MockParams) {
    this.fetchMock.mockOnceIf(url, '', response)
  }

  expectCall(url: string) {
    expect(this.fetchMock).toHaveBeenCalledWith(
      url,
      expect.anything()
    )
  }
}

export default ApiMock
