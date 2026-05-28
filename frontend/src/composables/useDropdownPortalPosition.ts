import { computed, nextTick, onUnmounted, ref, watch, type Ref } from "vue"
import {
  computeDropdownPortalStyle,
  dropdownPortalPanelFallbackSize,
  parseDropdownPlacementFromDetails,
} from "./dropdownPortalPlacement"

export type {
  DropdownPortalPlacement,
  DropdownPortalSide,
} from "./dropdownPortalPlacement"

export {
  computeDropdownPortalStyle,
  parseDropdownPlacementFromDetails,
} from "./dropdownPortalPlacement"

export function useDropdownPortalPosition(options: {
  open: Ref<boolean>
  detailsRef: Ref<HTMLDetailsElement | null>
  panelRef: Ref<HTMLElement | null>
}) {
  const { open, detailsRef, panelRef } = options
  const positionStyle = ref<{ top: string; left: string } | null>(null)

  const placement = computed(() =>
    parseDropdownPlacementFromDetails(detailsRef.value)
  )

  const updatePosition = () => {
    const details = detailsRef.value
    const panel = panelRef.value
    if (!details || !panel || !open.value) {
      positionStyle.value = null
      return
    }

    const summary = details.querySelector("summary")
    if (!summary) {
      positionStyle.value = null
      return
    }

    const anchorRect = summary.getBoundingClientRect()
    const panelWidth =
      panel.offsetWidth || dropdownPortalPanelFallbackSize.width
    const panelHeight =
      panel.offsetHeight || dropdownPortalPanelFallbackSize.height
    positionStyle.value = computeDropdownPortalStyle(
      anchorRect,
      panelWidth,
      panelHeight,
      placement.value
    )
  }

  let listenersAttached = false

  const attachListeners = () => {
    if (listenersAttached) return
    window.addEventListener("resize", updatePosition)
    window.addEventListener("scroll", updatePosition, true)
    listenersAttached = true
  }

  const detachListeners = () => {
    if (!listenersAttached) return
    window.removeEventListener("resize", updatePosition)
    window.removeEventListener("scroll", updatePosition, true)
    listenersAttached = false
  }

  watch(
    () => open.value,
    async (isOpen) => {
      if (isOpen) {
        await nextTick()
        updatePosition()
        attachListeners()
        await nextTick()
        updatePosition()
      } else {
        detachListeners()
        positionStyle.value = null
      }
    }
  )

  watch(placement, () => {
    if (open.value) {
      updatePosition()
    }
  })

  onUnmounted(() => {
    detachListeners()
  })

  return { positionStyle }
}
