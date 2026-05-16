import { onMounted, onUnmounted, type Ref } from "vue"

export function useAutoCollapseDetails(
  detailsRef: Ref<HTMLDetailsElement | null>
) {
  const closeDetails = () => {
    if (detailsRef.value) {
      detailsRef.value.open = false
    }
  }

  const isInsideModal = (target: EventTarget | null) =>
    target instanceof HTMLElement &&
    Boolean(target.closest("dialog.modal-mask"))

  const shouldCloseForTarget = (target: EventTarget | null) =>
    detailsRef.value?.open === true &&
    target instanceof Node &&
    !detailsRef.value.contains(target) &&
    !isInsideModal(target)

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
