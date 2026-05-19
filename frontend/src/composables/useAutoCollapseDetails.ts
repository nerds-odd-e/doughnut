import { onMounted, onUnmounted, type Ref } from "vue"

export function useAutoCollapseDetails(
  detailsRef: Ref<HTMLDetailsElement | null>,
  closeDetails: () => void = () => {
    if (detailsRef.value) {
      detailsRef.value.open = false
    }
  }
) {
  const isInsideAnotherModal = (target: EventTarget | null) => {
    if (!(target instanceof HTMLElement)) return false
    const targetModal = target.closest("dialog.modal-mask")
    const ownModal = detailsRef.value?.closest("dialog.modal-mask")
    return targetModal != null && targetModal !== ownModal
  }

  const shouldCloseForTarget = (target: EventTarget | null) =>
    detailsRef.value?.open === true &&
    target instanceof Node &&
    !detailsRef.value.contains(target) &&
    !isInsideAnotherModal(target)

  const handleDocumentClick = (event: MouseEvent) => {
    if (shouldCloseForTarget(event.target)) {
      closeDetails()
    }
  }

  onMounted(() => {
    document.addEventListener("click", handleDocumentClick)
  })

  onUnmounted(() => {
    document.removeEventListener("click", handleDocumentClick)
  })

  return { closeDetails }
}
