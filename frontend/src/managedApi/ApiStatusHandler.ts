import { useToast } from "vue-toastification"

export type ApiStatus = {
  states: boolean[]
}

export default class ApiStatusHandler {
  apiStatus: ApiStatus
  private toast = useToast()

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

  addError(message: string, statusCode?: number): void {
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
