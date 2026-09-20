import { nextTick, onMounted, onUnmounted, ref, watch, type Ref } from "vue"
import {
  computePasteChoiceStyle,
  type PasteChoiceAnchorRect,
} from "./pasteChoicePosition"

const HIDDEN_STYLE = { top: "0px", left: "0px", visibility: "hidden" as const }

/** Positions the paste-choice action bar with `position: fixed`, fit to the viewport and
 * clear of `anchorRect` (the just-pasted content's own geometry). Hidden until the bar's
 * own rendered size is known (it must exist in the DOM to be measured), then recomputed
 * on window resize while visible. */
export function usePasteChoicePosition(
  anchorRect: Ref<PasteChoiceAnchorRect | null | undefined>,
  barRef: Ref<HTMLElement | null>
) {
  const positionStyle = ref<{
    top: string
    left: string
    visibility: "visible" | "hidden"
  }>(HIDDEN_STYLE)

  const update = () => {
    const anchor = anchorRect.value
    const bar = barRef.value
    if (!anchor || !bar) {
      positionStyle.value = HIDDEN_STYLE
      return
    }
    const rect = bar.getBoundingClientRect()
    positionStyle.value = {
      ...computePasteChoiceStyle(
        anchor,
        { width: rect.width, height: rect.height },
        { width: window.innerWidth, height: window.innerHeight }
      ),
      visibility: "visible",
    }
  }

  watch(anchorRect, () => {
    positionStyle.value = HIDDEN_STYLE
    nextTick(update)
  })

  onMounted(() => window.addEventListener("resize", update))
  onUnmounted(() => window.removeEventListener("resize", update))

  return { positionStyle }
}
