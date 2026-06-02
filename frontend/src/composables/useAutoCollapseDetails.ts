import { onMounted, onUnmounted, type Ref } from "vue"
import {
  dropdownPortalPanelSelector,
  eventClickTarget,
} from "./dropdownPortalContext"

export function useAutoCollapseDetails(
  detailsRef: Ref<HTMLDetailsElement | null>,
  closeDetails: () => void = () => {
    if (detailsRef.value) {
      detailsRef.value.open = false
    }
  },
  portalId?: string
) {
  const isInsideAnotherModal = (target: EventTarget | null) => {
    const element = eventClickTarget(target)
    if (!element) return false
    const targetModal = element.closest("dialog.modal-mask")
    const ownModal = detailsRef.value?.closest("dialog.modal-mask")
    return targetModal != null && targetModal !== ownModal
  }

  const isInsidePortaledPanel = (target: EventTarget | null) => {
    if (portalId == null) return false
    return (
      eventClickTarget(target)?.closest(
        dropdownPortalPanelSelector(portalId)
      ) != null
    )
  }

  const shouldCloseForTarget = (target: EventTarget | null) =>
    detailsRef.value?.open === true &&
    target instanceof Node &&
    !detailsRef.value.contains(target) &&
    !isInsidePortaledPanel(target) &&
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
