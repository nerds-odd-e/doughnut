import Quill, { Delta } from "quill"
import type { DictationTarget } from "@/models/audio/dictationTarget"
import { dictatedInsertion } from "@/models/audio/joinDictatedSegments"

/** The given caret or selection, or the end of the text, becomes a dictation target and Quill is disabled until it ends. */
export function quillDictationTarget(
  quill: Quill,
  range: { index: number; length: number } | null
): DictationTarget {
  let { index, length } = range ?? { index: quill.getLength() - 1, length: 0 }
  quill.enable(false)
  return {
    insert: (segments) => {
      const text = dictatedInsertion(
        quill.getText(0, index),
        segments,
        quill.getText(index + length)
      )
      quill.updateContents(
        new Delta().retain(index).delete(length).insert(text),
        Quill.sources.API
      )
      index += text.length
      length = 0
    },
    end: (placeCaret) => {
      quill.enable(true)
      if (placeCaret) quill.setSelection(index, 0, Quill.sources.API)
    },
  }
}
