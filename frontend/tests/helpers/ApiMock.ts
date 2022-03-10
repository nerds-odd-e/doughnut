import { FetchMock, MockParams } from "jest-fetch-mock";

class ApiMock {
  fetchMock
  private unexpectedApiCalls: string[] = []
  private expected: {url: string, value: any, response?: MockParams, called: boolean}[] = []

  constructor(fetchMock: FetchMock) {
    this.fetchMock = fetchMock
    this.fetchMock.doMock(async (request: Request)=>{
      const match = this.expected.filter(exp=>exp.url === request.url).pop()
      if(match) {
        match.called = true
        return match.response || JSON.stringify(match.value)
      }

      this.unexpectedApiCalls.push(request.url)
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
    this.expected.push({ url, value, called: false })
  }

  mockResponse(url: string, response: MockParams) {
    this.expected.push({ url, value: {}, response, called: false })
  }

  expectCall(url: string) {
    expect(this.fetchMock).toHaveBeenCalledWith(
      url,
      expect.anything()
    )
  }
}

export default ApiMock
