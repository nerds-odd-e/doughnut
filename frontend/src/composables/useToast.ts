export function useToast() {
  const showSuccessToast = (message: string) => {
    // Implement your toast logic here
    console.log("Success:", message)
  }

  const showErrorToast = (message: string) => {
    // Implement your toast logic here
    console.log("Error:", message)
  }

  return {
    showSuccessToast,
    showErrorToast,
  }
}
