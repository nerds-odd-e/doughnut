import {
  BOOK_LAYOUT_BLOCK_DRAG_THRESHOLD_PX,
  bookLayoutBlockDragIntent,
  bookLayoutBlockDragShouldCapture,
} from "@/lib/book-reading/bookLayoutBlockDragIntent"
import type { BookBlockFull } from "@generated/donut-backend-api"
import { ref } from "vue"

const dragThresholdOpts = { thresholdPx: BOOK_LAYOUT_BLOCK_DRAG_THRESHOLD_PX }

/** Horizontal drag on a book layout row: right indents, left outdents. */
export function useBookLayoutBlockPointerDrag(handlers: {
  indent: (block: BookBlockFull) => void
  outdent: (block: BookBlockFull) => void
}) {
  const drag = ref<{
    blockId: number
    startX: number
    startY: number
    pointerId: number
    captured: boolean
  } | null>(null)

  const suppressNextClick = ref(false)

  function activeDrag(block: BookBlockFull, e: PointerEvent) {
    const s = drag.value
    if (!s || s.pointerId !== e.pointerId || s.blockId !== block.id) {
      return null
    }
    return s
  }

  /** True when the click ends a drag and must not select the block. */
  function consumeDragClick(e: MouseEvent): boolean {
    if (!suppressNextClick.value) {
      return false
    }
    suppressNextClick.value = false
    e.preventDefault()
    e.stopPropagation()
    return true
  }

  function onPointerDown(block: BookBlockFull, e: PointerEvent) {
    if (e.pointerType === "mouse" && e.button !== 0) {
      return
    }
    drag.value = {
      blockId: block.id,
      startX: e.clientX,
      startY: e.clientY,
      pointerId: e.pointerId,
      captured: false,
    }
  }

  function onPointerMove(block: BookBlockFull, e: PointerEvent) {
    const s = activeDrag(block, e)
    if (!s) {
      return
    }
    const dx = e.clientX - s.startX
    const dy = e.clientY - s.startY
    if (
      !s.captured &&
      bookLayoutBlockDragShouldCapture(dx, dy, dragThresholdOpts)
    ) {
      try {
        ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
      } catch {
        /* Synthetic pointer events in tests may not have an active pointer id. */
      }
      drag.value = { ...s, captured: true }
    }
  }

  function endDrag(block: BookBlockFull, e: PointerEvent) {
    const s = activeDrag(block, e)
    if (!s) {
      return null
    }
    if (s.captured) {
      try {
        ;(e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId)
      } catch {
        /* ignore */
      }
    }
    drag.value = null
    return s
  }

  function onPointerUp(block: BookBlockFull, e: PointerEvent) {
    const s = endDrag(block, e)
    if (!s) {
      return
    }
    const intent = bookLayoutBlockDragIntent(
      e.clientX - s.startX,
      e.clientY - s.startY,
      dragThresholdOpts
    )
    if (intent === "INDENT") {
      suppressNextClick.value = true
      handlers.indent(block)
    } else if (intent === "OUTDENT") {
      suppressNextClick.value = true
      handlers.outdent(block)
    }
  }

  function onPointerCancel(block: BookBlockFull, e: PointerEvent) {
    endDrag(block, e)
  }

  return {
    consumeDragClick,
    onPointerDown,
    onPointerMove,
    onPointerUp,
    onPointerCancel,
  }
}
