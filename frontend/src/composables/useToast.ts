import { useToast as useVueToast } from "vue-toastification"

export function useToast() {
  const toast = useVueToast()

  const showSuccessToast = (message: string, options = {}) => {
    toast.success(message, options)
  }

  const showErrorToast = (message: string, options = {}) => {
    toast.error(message, options)
  }

  return {
    showSuccessToast,
    showErrorToast,
  }
}
