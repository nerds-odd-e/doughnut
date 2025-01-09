import { useToast as useVueToast } from "vue-toastification"

export function useToast() {
  const toast = useVueToast()

  const showSuccessToast = (message: string, options = {}) => {
    toast.success(message, {
      ...options,
      container: { "data-testid": "toast-message" },
    })
  }

  const showErrorToast = (message: string, options = {}) => {
    toast.error(message, {
      ...options,
      container: { "data-testid": "toast-message" },
    })
  }

  return {
    showSuccessToast,
    showErrorToast,
  }
}
