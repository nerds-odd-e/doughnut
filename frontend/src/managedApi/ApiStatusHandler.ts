export type ApiStatus = {
  states: boolean[]
}

export default class ApiStatusHandler {
  apiStatus: ApiStatus

  constructor(apiStatus: ApiStatus) {
    this.apiStatus = apiStatus
  }

  assignLoading(value: boolean) {
    if (value) {
      this.apiStatus.states.push(true)
    } else {
      this.apiStatus.states.pop()
    }
  }
}
