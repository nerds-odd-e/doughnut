import { nextTick, onMounted, onUnmounted, ref, watch, type Ref } from "vue"
import type { QuillPasteContext } from "@/components/form/QuillEditor.vue"
import type TextArea from "@/components/form/TextArea.vue"
import { usePasteWithLinkImageOptions } from "@/composables/usePasteWithLinkImageOptions"
import { countMarkdownLinksAndImagesInNoteContent } from "@/utils/stripPastedMarkdownLinks"

type NoteContentUpdate = (noteId: number, newValue: string) => void

/** A just-completed Markdown-converting paste that can still be replaced with the original clipboard text. */
export type PasteChoice = {
  originalText: string
  replace: () => void
}

const EXPIRY_MS = 10_000

export function useNoteContentPaste(options: {
  noteId: () => number
  asMarkdown: () => boolean
  noteContent: () => string | undefined
  rootRef: Ref<HTMLElement | null>
  textareaRef: Ref<InstanceType<typeof TextArea> | null>
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
    if (contentOpensLinkImagePrompt(content)) {
      clearPasteChoice()
    }
    const processedContent = await processContentAfterPaste(content)
    if (processedContent !== null) {
      lastAppliedValue = processedContent
      update(options.noteId(), processedContent)
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
      pasteChoice.value = {
        originalText,
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
      }
      startExpiryTimer()
    } else {
      clearPasteChoice()
    }

    await offerToRemoveLinksAndImages(newValue, update)
  }

  /** `_quillContext` is captured for slice 4's rich-mode correction; not yet consumed. */
  const handlePasteComplete = async (
    currentValue: string | undefined,
    update: NoteContentUpdate,
    _quillContext: QuillPasteContext | null
  ) => {
    if (!currentValue) return
    await offerToRemoveLinksAndImages(currentValue, update)
  }

  /** Invalidates the choice on an externally-driven note content change; a save-round-trip echo of our own value is not one. */
  const syncNoteContent = (noteContent: string | undefined) => {
    if (pasteChoice.value && noteContent !== lastAppliedValue) {
      clearPasteChoice()
    }
  }

  /** Real user typing/undo (or a programmatic insertion elsewhere) invalidates a pending choice. */
  const handleTextareaModelUpdate = (
    update: NoteContentUpdate,
    newValue: string
  ) => {
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
    handleTextareaModelUpdate,
    handleTextareaPaste,
    handlePasteComplete,
  }
}
