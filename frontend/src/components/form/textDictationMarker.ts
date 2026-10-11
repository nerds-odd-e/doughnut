import type { DictationTarget } from "@/models/audio/dictationTarget"
import { withDictationMarker } from "./dictationMarker"

/**
 * Shows the dictation marker after the character offset `end()` of the one
 * text node `editor` keeps. The marker is laid over the element around the
 * editor, which is positioned, because the editor rewrites its own children.
 */
export const withMarkerAfterTextOffset = (
  target: DictationTarget,
  editor: HTMLElement,
  end: () => number
): DictationTarget =>
  withDictationMarker(
    {
      ...target,
      anchorRect: () => {
        // An empty editor has no text to measure: a zero-width character stands in for the measurement.
        const standIn = editor.firstChild
          ? undefined
          : editor.appendChild(new Text("\u200b"))
        const text = editor.firstChild as Text
        const range = document.createRange()
        range.setStart(text, Math.min(end(), text.length))
        const rect = range.getBoundingClientRect()
        standIn?.remove()
        return rect
      },
    },
    editor.parentElement!
  )
