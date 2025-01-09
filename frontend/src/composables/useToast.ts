import { useToast as useVueToast } from "vue-toastification"

export function useToast() {
  const toast = useVueToast()

  const showSuccessToast = (message: string) => {
    toast.success(message)
  }

  const showErrorToast = (message: string) => {
    toast.error(message)
  }

  return {
    showSuccessToast,
    showErrorToast,
  }
}
