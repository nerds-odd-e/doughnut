export type ApiLoadingState = {
  id: number
  blockUi?: boolean
  message?: string
}

export type ApiStatus = {
  states: ApiLoadingState[]
}

export type ApiLoadingOptions = {
  blockUi?: boolean
  message?: string
}

export default class ApiStatusHandler {
  apiStatus: ApiStatus

  private nextLoadingStateId = 1

  constructor(apiStatus: ApiStatus) {
    this.apiStatus = apiStatus
  }

  startLoading(options: ApiLoadingOptions = {}) {
    const loadingState = {
      id: this.nextLoadingStateId++,
      blockUi: options.blockUi,
      message: options.message,
    }
    this.apiStatus.states.push(loadingState)
    return loadingState
  }

  finishLoading(loadingState: ApiLoadingState) {
    this.apiStatus.states = this.apiStatus.states.filter(
      (state) => state.id !== loadingState.id
    )
  }
}

export function currentBlockingApiState(apiStatus: ApiStatus) {
  for (let i = apiStatus.states.length - 1; i >= 0; i--) {
    const state = apiStatus.states[i]
    if (state?.blockUi) {
      return state
    }
  }
  return
}
