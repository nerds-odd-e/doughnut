import fetchMock, { MockParams } from "jest-fetch-mock";

class ApiMockImpl {
  fetchMock = fetchMock

  private unexpectedApiCalls: string[] = []

  private expected: {url: string, value: any, response?: MockParams, called: boolean}[] = []

  init() {
    this.fetchMock.doMock(async (request: Request)=>{
      const match = this.expected.filter(exp=>exp.url === request.url).pop()
      if(match) {
        match.called = true
        return match.response || JSON.stringify(match.value)
      }

      this.unexpectedApiCalls.push(request.url)
      return JSON.stringify({})
    })
    return this
  }

  noUnexpectedCalls() {
    expect(this.unexpectedApiCalls).toHaveLength(0)
    expect(this.mismatchedApiCalls).toHaveLength(0)
  }

  private get mismatchedApiCalls(): string[] {
    return this.expected.filter(exp => !exp.called).map(exp => exp.url);
  }

  expecting(url: string, value: any={}) {
    this.expected.push({ url, value, called: false })
  }

  expectingResponse(url: string, response: MockParams) {
    this.expected.push({ url, value: {}, response, called: false })
  }

  verifyCall(url: string, matcher: any=expect.anything()) {
    const unexpectedIndex = this.unexpectedApiCalls.indexOf(url)
    unexpectedIndex >=0 && this.unexpectedApiCalls.splice(unexpectedIndex, 1)

    expect(this.fetchMock).toHaveBeenCalledWith(
      url,
      matcher
    )
  }
}

interface ApiMock {
  expecting(url: string) : void
  expecting(url: string, value: any) : void
  expectingResponse(url: string, response: MockParams) : void
  verifyCall(url: string) : void
  verifyCall(url: string, matcher: any) : void
}

const  setupApiMock = () => {
  const mockedApi = new ApiMockImpl().init()
  return {mockedApi: mockedApi as ApiMock, teardown: ()=> mockedApi.noUnexpectedCalls()}
}

export { setupApiMock }

export default ApiMock