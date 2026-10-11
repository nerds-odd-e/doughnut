import { joinDictatedSegments } from "./joinDictatedSegments"

/** Where one voice-input session puts its text: the caret or selection an input had when the session began. The input takes no edits until the session ends. */
export type DictationTarget = {
  /** Puts the segments at the target by the dictation join rule; the next ones follow them. */
  insert: (segments: readonly string[]) => void
  /** Makes the input editable again, with focus and the caret after the inserted text when `placeCaret`. */
  end: (placeCaret: boolean) => void
}

/** A dictation target that reports where its next text will arrive: the place right after its caret, selection, or last inserted text, in viewport coordinates. */
export type AnchoredDictationTarget = DictationTarget & {
  anchorRect: () => { left: number; top: number; height: number }
}

/** The end of a text whose input is not editable to begin with. */
export const endOfTextDictationTarget = (
  text: () => string,
  replaceText: (text: string) => void
): DictationTarget => ({
  insert: (segments) => replaceText(joinDictatedSegments(text(), segments)),
  end: () => undefined,
})
