import { useToast } from "vue-toastification"

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
  private toast = useToast()
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
    if (this.silentMode) return
    this.toast.error(message)
  }
}
