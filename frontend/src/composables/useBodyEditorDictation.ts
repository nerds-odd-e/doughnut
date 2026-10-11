import { watch, type Ref } from "vue"
import type { DictationTarget } from "@/models/audio/dictationTarget"
import { dictatedInsertion } from "@/models/audio/joinDictatedSegments"

type TextareaSelection = { start: number; end: number }

/** The selection, or the end of the text, becomes a dictation target and the textarea is read-only until it ends. */
function textareaDictationTarget(
  textarea: HTMLTextAreaElement,
  selection: Ref<TextareaSelection | null>
): DictationTarget {
  let { start, end } = selection.value ?? {
    start: textarea.value.length,
    end: textarea.value.length,
  }
  textarea.readOnly = true
  return {
    insert: (segments) => {
      const before = textarea.value.slice(0, start)
      const after = textarea.value.slice(end)
      const text = dictatedInsertion(before, segments, after)
      textarea.value = before + text + after
      textarea.dispatchEvent(new Event("input", { bubbles: true }))
      start = end = before.length + text.length
    },
    end: (placeCaret) => {
      textarea.readOnly = false
      if (!placeCaret) return
      textarea.focus()
      textarea.setSelectionRange(start, end)
      selection.value = { start, end }
    },
  }
}

/** One dictation session for the note body, in whichever of the Markdown textarea and the rich editor is shown. */
export function useBodyEditorDictation(editors: {
  asMarkdown: () => boolean
  markdownTextarea: () => HTMLTextAreaElement
  textareaSelection: Ref<TextareaSelection | null>
  beginRichDictation: () => DictationTarget
}) {
  const beginInShownEditor = () =>
    editors.asMarkdown()
      ? textareaDictationTarget(
          editors.markdownTextarea(),
          editors.textareaSelection
        )
      : editors.beginRichDictation()

  let session: DictationTarget | undefined

  // A session goes on in the editor the author switches to.
  watch(
    editors.asMarkdown,
    () => {
      if (session) session = beginInShownEditor()
    },
    { flush: "post" }
  )

  return (): DictationTarget => {
    session = beginInShownEditor()
    return {
      insert: (segments) => session!.insert(segments),
      end: (placeCaret) => {
        session!.end(placeCaret)
        session = undefined
      },
    }
  }
}
