import { nextTick, onMounted, onUnmounted, ref, watch, type Ref } from "vue"
import type { QuillPasteContext } from "@/components/form/quillPasteContext"
import type TextArea from "@/components/form/TextArea.vue"
import { usePasteWithLinkImageOptions } from "@/composables/usePasteWithLinkImageOptions"
import {
  toPasteChoiceAnchorRect,
  type PasteChoiceAnchorRect,
} from "@/composables/pasteChoicePosition"
import { countMarkdownLinksAndImagesInNoteContent } from "@/utils/stripPastedMarkdownLinks"

type NoteContentUpdate = (noteId: number, newValue: string) => void

/** A just-completed Markdown-converting paste that can still be replaced with the original
 * clipboard text. `anchorRect` is the just-pasted content's own viewport geometry, captured
 * at paste time, used to keep the action reachable without covering that content. */
export type PasteChoice = {
  originalText: string
  anchorRect: PasteChoiceAnchorRect | null
  replace: () => void
}

const EXPIRY_MS = 10_000

export function useNoteContentPaste(options: {
  noteId: () => number
  asMarkdown: () => boolean
  noteContent: () => string | undefined
  rootRef: Ref<HTMLElement | null>
  textareaRef: Ref<InstanceType<typeof TextArea> | null>
  replacePastedRange: (context: QuillPasteContext, text: string) => void
  getRichPasteAnchorRect: (range: {
    index: number
    length: number
  }) => PasteChoiceAnchorRect | null
}) {
  const { htmlToMarkdown, processContentAfterPaste } =
    usePasteWithLinkImageOptions()

  const pasteChoice = ref<PasteChoice | null>(null)
  /** The value this composable itself last produced, to tell a save-round-trip echo from an actual external change. */
  let lastAppliedValue: string | undefined
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

  const contentOpensLinkImagePrompt = (content: string): boolean => {
    const { linkCount, imageCount } =
      countMarkdownLinksAndImagesInNoteContent(content)
    return linkCount > 0 || imageCount > 0
  }

  const offerToRemoveLinksAndImages = async (
    content: string,
    update: NoteContentUpdate
  ) => {
    // Suspend the current choice (and its timer) while the modal is open, rather than
    // discarding it: cancellation below resumes it, removal consumes it.
    const suspendedChoice = contentOpensLinkImagePrompt(content)
      ? pasteChoice.value
      : null
    if (suspendedChoice) clearPasteChoice()

    const processedContent = await processContentAfterPaste(content)
    if (processedContent !== null) {
      lastAppliedValue = processedContent
      update(options.noteId(), processedContent)
    } else if (suspendedChoice && pasteChoice.value === null) {
      // Nothing else claimed pasteChoice while the modal was open, so it is still current.
      setPasteChoice(suspendedChoice)
    }
  }

  const handleTextareaPaste = async (
    event: ClipboardEvent,
    currentValue: string | undefined,
    update: NoteContentUpdate
  ) => {
    if (!options.asMarkdown() || !options.textareaRef.value) return

    const htmlData = event.clipboardData?.getData("text/html")
    if (!htmlData) return

    event.preventDefault()

    const textarea = options.textareaRef.value?.$el?.querySelector(
      "textarea"
    ) as HTMLTextAreaElement | null
    if (!textarea) return

    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const originalText = event.clipboardData?.getData("text/plain") ?? ""
    const markdown = htmlToMarkdown(htmlData)
    const before = (currentValue || "").slice(0, start)
    const after = (currentValue || "").slice(end)
    const newValue = before + markdown + after

    lastAppliedValue = newValue
    update(options.noteId(), newValue)
    nextTick(() => {
      textarea.selectionStart = textarea.selectionEnd =
        before.length + markdown.length
    })

    if (originalText && originalText !== markdown) {
      setPasteChoice({
        originalText,
        anchorRect: toPasteChoiceAnchorRect(textarea.getBoundingClientRect()),
        replace: () => {
          const replacedValue = before + originalText + after
          lastAppliedValue = replacedValue
          update(options.noteId(), replacedValue)
          clearPasteChoice()
          nextTick(() => {
            textarea.selectionStart = textarea.selectionEnd =
              before.length + originalText.length
          })
        },
      })
    } else {
      clearPasteChoice()
    }

    await offerToRemoveLinksAndImages(newValue, update)
  }

  /** A genuine original clipboard text is one the rich conversion actually lost:
   * non-blank, and no longer present verbatim in the composed note content
   * (Quill's Delta insertion index doesn't map onto a Markdown character
   * offset the way the textarea's own selection does, so this checks the
   * composed result rather than slicing a range out of it). */
  const quillPasteLostOriginalText = (
    currentValue: string,
    quillContext: QuillPasteContext
  ): boolean =>
    quillContext.originalText.trim() !== "" &&
    !currentValue.includes(quillContext.originalText)

  const handlePasteComplete = async (
    currentValue: string | undefined,
    update: NoteContentUpdate,
    quillContext: QuillPasteContext | null
  ) => {
    if (!currentValue) return

    if (
      quillContext &&
      quillPasteLostOriginalText(currentValue, quillContext)
    ) {
      lastAppliedValue = currentValue
      setPasteChoice({
        originalText: quillContext.originalText,
        anchorRect: options.getRichPasteAnchorRect({
          index: quillContext.range.index,
          length: quillContext.insertedLength,
        }),
        replace: () => {
          options.replacePastedRange(quillContext, quillContext.originalText)
          clearPasteChoice()
        },
      })
    } else {
      clearPasteChoice()
    }

    await offerToRemoveLinksAndImages(currentValue, update)
  }

  /** Invalidates the choice on an externally-driven note content change; a save-round-trip echo of our own value is not one. */
  const syncNoteContent = (noteContent: string | undefined) => {
    if (pasteChoice.value && noteContent !== lastAppliedValue) {
      clearPasteChoice()
    }
  }

  /** Real user typing/undo (or a programmatic insertion elsewhere), in either editor mode,
   * invalidates a pending choice. Also fires for our own `replace()`-driven content update
   * in the rich path, which is harmless since `replace()` clears the choice itself too. */
  const handleModelUpdate = (update: NoteContentUpdate, newValue: string) => {
    clearPasteChoice()
    update(options.noteId(), newValue)
  }

  const handleEscapeKey = (event: KeyboardEvent) => {
    if (event.key === "Escape") clearPasteChoice()
  }

  const handleOutsideInteraction = (event: MouseEvent) => {
    if (!pasteChoice.value) return
    if (
      event.target instanceof Node &&
      options.rootRef.value?.contains(event.target)
    ) {
      return
    }
    clearPasteChoice()
  }

  watch(
    () => [options.noteId(), options.asMarkdown()] as const,
    clearPasteChoice
  )
  watch(() => options.noteContent(), syncNoteContent)

  onMounted(() => {
    document.addEventListener("keydown", handleEscapeKey)
    document.addEventListener("mousedown", handleOutsideInteraction)
  })

  onUnmounted(() => {
    document.removeEventListener("keydown", handleEscapeKey)
    document.removeEventListener("mousedown", handleOutsideInteraction)
    clearPasteChoice()
  })

  return {
    pasteChoice,
    clearPasteChoice,
    pausePasteChoiceExpiry,
    resumePasteChoiceExpiry,
    handleModelUpdate,
    handleTextareaPaste,
    handlePasteComplete,
  }
}
