import { ref } from "vue"
import type { PasteChoiceAnchorRect } from "@/composables/pasteChoicePosition"

/** A just-completed Markdown-converting paste that can still be replaced with the original
 * clipboard text. `anchorRect` is the just-pasted content's own viewport geometry, captured
 * at paste time, used to keep the action reachable without covering that content. */
export type PasteChoice = {
  originalText: string
  anchorRect: PasteChoiceAnchorRect | null
  replace: () => void
}

const EXPIRY_MS = 10_000

/** The offered paste choice, which expires unless the author is hovering or focusing it. */
export function usePasteChoice() {
  const pasteChoice = ref<PasteChoice | null>(null)
  let expiryTimer: ReturnType<typeof setTimeout> | undefined

  const stopExpiryTimer = () => {
    clearTimeout(expiryTimer)
    expiryTimer = undefined
  }

  const clearPasteChoice = () => {
    stopExpiryTimer()
    pasteChoice.value = null
  }

  const startExpiryTimer = () => {
    stopExpiryTimer()
    expiryTimer = setTimeout(clearPasteChoice, EXPIRY_MS)
  }

  /** Both paste-completion sites (Markdown textarea and rich Quill) offer their choice the same way. */
  const setPasteChoice = (choice: PasteChoice) => {
    pasteChoice.value = choice
    startExpiryTimer()
  }

  /** Hover or keyboard focus on the action pauses expiry until it is left/blurred. */
  const pausePasteChoiceExpiry = () => {
    if (pasteChoice.value) stopExpiryTimer()
  }

  const resumePasteChoiceExpiry = () => {
    if (pasteChoice.value) startExpiryTimer()
  }

  return {
    pasteChoice,
    setPasteChoice,
    clearPasteChoice,
    pausePasteChoiceExpiry,
    resumePasteChoiceExpiry,
  }
}
