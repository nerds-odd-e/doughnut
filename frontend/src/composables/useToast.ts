import { useToast as useVueToast } from "vue-toastification"

export function useToast() {
  const toast = useVueToast()

  const showSuccessToast = (message: string, options = {}) => {
    const toastElement = toast.success(message, {
      ...options,
      container: { "data-testid": "toast-message" },
    })
  }

  const showErrorToast = (message: string, options = {}) => {
    const toastElement = toast.error(message, {
      ...options,
      container: { "data-testid": "toast-message" },
    })
  }

  return {
    showSuccessToast,
    showErrorToast,
  }
}
