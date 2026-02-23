const modalStack: Array<() => void> = []
let isListenerAttached = false

function handleEscape(event: KeyboardEvent) {
  if (event.key !== "Escape" || modalStack.length === 0) return

  event.preventDefault()
  event.stopPropagation()
  const close = modalStack.pop()!
  close()
}

function attachListener() {
  if (!isListenerAttached) {
    document.addEventListener("keydown", handleEscape)
    isListenerAttached = true
  }
}

function detachListener() {
  if (isListenerAttached && modalStack.length === 0) {
    document.removeEventListener("keydown", handleEscape)
    isListenerAttached = false
  }
}

export function registerModal(close: () => void): () => void {
  modalStack.push(close)
  attachListener()
  return () => {
    const i = modalStack.indexOf(close)
    if (i >= 0) modalStack.splice(i, 1)
    detachListener()
  }
}
