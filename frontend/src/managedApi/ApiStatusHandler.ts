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

  addError(message: string, statusCode?: number): void {
    if (this.silentMode) return

    // For 404 errors, show longer timeout and make it more visible
    const timeout = statusCode === 404 ? 15000 : 3000 // 15 seconds for 404, 3 seconds for others

    this.toast.error(message, {
      timeout,
      closeOnClick: false, // Prevent accidental dismissal for 404 errors
      pauseOnFocusLoss: true,
      pauseOnHover: true,
    })
  }
}
