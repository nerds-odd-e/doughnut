export type ApiError = {
  id: number
  message: string
}

export type ApiStatus = {
  states: boolean[]
  errors: ApiError[]
}

export default class ApiStatusHandler {
  apiStatus: ApiStatus

  private silentMode?: boolean

  constructor(apiStatus: ApiStatus, silent?: boolean) {
    this.apiStatus = apiStatus
    this.silentMode = silent
  }

  assignLoading(value: boolean) {
    if (this.silentMode) return
    if (value) {
      this.apiStatus.states.push(true)
    } else {
      this.apiStatus.states.pop()
    }
  }

  addError(message: string): void {
    const id = Date.now()
    this.apiStatus.errors.push({ message, id })
    setTimeout(() => {
      this.apiStatus.errors = this.apiStatus.errors.filter(
        (error) => error.id !== id
      )
    }, 2000)
  }
}
