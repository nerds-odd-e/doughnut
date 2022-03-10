import { FetchMock } from "jest-fetch-mock";

class ApiMock {
  private fetchMock
  private unexpected: string[] = []

  constructor(fetchMock: FetchMock) {
    this.fetchMock = fetchMock
    this.fetchMock.doMock(async (request: Request)=>{
      this.unexpected.push(request.url)
      // use empty json as default return value,
      // hopefully, the execution will be back to the
      // main thread and the exception makes more sense.
      return JSON.stringify({})
    })
  }

  noUnexpectedCalls() {
    expect(this.unexpected).toHaveLength(0)
  }

  mockResponseOnce(url: string, value: any={}) {
    this.fetchMock.mockOnceIf(url, async (request: Request)=>{
      console.log(url)
      return JSON.stringify(value)
    })
  }

  expectTimes(times: number, url: string) {
    expect(this.fetchMock).toHaveBeenCalledTimes(times);
    expect(this.fetchMock).toHaveBeenCalledWith(
      url,
      expect.anything()
    )
  }

  expectOnce(url: string) {
    this.expectTimes(1, url)
  }

}

export default ApiMock
