import { FetchMock } from "jest-fetch-mock";

class ApiMock {
  private fetchMock

  constructor(fetchMock: FetchMock) {
    this.fetchMock = fetchMock
  }

  mockResponseOnce(url: string, value: any) {
    this.fetchMock.mockResponseOnce(JSON.stringify(value))
  }

  expectOnce(url: string) {
    expect(this.fetchMock).toHaveBeenCalledTimes(1);
    expect(this.fetchMock).toHaveBeenCalledWith(
      url,
      expect.anything()
    )
  }
}

export default ApiMock
