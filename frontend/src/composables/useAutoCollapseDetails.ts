import { onMounted, onUnmounted, type Ref } from "vue"

export function useAutoCollapseDetails(
  detailsRef: Ref<HTMLDetailsElement | null>
) {
  const closeDetails = () => {
    if (detailsRef.value) {
      detailsRef.value.open = false
    }
  }

  const shouldCloseForTarget = (target: EventTarget | null) =>
    detailsRef.value?.open === true &&
    target instanceof Node &&
    !detailsRef.value.contains(target)

  const handleDocumentClick = (event: MouseEvent) => {
    if (shouldCloseForTarget(event.target)) {
      closeDetails()
    }
  }

  const handleDocumentFocusIn = (event: FocusEvent) => {
    if (shouldCloseForTarget(event.target)) {
      closeDetails()
    }
  }

  onMounted(() => {
    document.addEventListener("click", handleDocumentClick)
    document.addEventListener("focusin", handleDocumentFocusIn)
  })

  onUnmounted(() => {
    document.removeEventListener("click", handleDocumentClick)
    document.removeEventListener("focusin", handleDocumentFocusIn)
  })

  return { closeDetails }
}
