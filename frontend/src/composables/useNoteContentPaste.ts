import { nextTick, type Ref } from "vue"
import type TextArea from "@/components/form/TextArea.vue"
import { usePasteWithLinkImageOptions } from "@/composables/usePasteWithLinkImageOptions"

type NoteContentUpdate = (noteId: number, newValue: string) => void

export function useNoteContentPaste(options: {
  noteId: () => number
  asMarkdown: () => boolean
  textareaRef: Ref<InstanceType<typeof TextArea> | null>
}) {
  const { htmlToMarkdown, processContentAfterPaste } =
    usePasteWithLinkImageOptions()

  const offerToRemoveLinksAndImages = async (
    content: string,
    update: NoteContentUpdate
  ) => {
    const processedContent = await processContentAfterPaste(content)
    if (processedContent !== null) {
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
    const markdown = htmlToMarkdown(htmlData)
    const newValue =
      (currentValue || "").slice(0, start) +
      markdown +
      (currentValue || "").slice(end)

    update(options.noteId(), newValue)
    nextTick(() => {
      if (textarea) {
        const afterLength = (currentValue || "").slice(end).length
        textarea.selectionStart = textarea.selectionEnd =
          newValue.length - afterLength
      }
    })

    await offerToRemoveLinksAndImages(newValue, update)
  }

  const handlePasteComplete = async (
    currentValue: string | undefined,
    update: NoteContentUpdate
  ) => {
    if (!currentValue) return
    await offerToRemoveLinksAndImages(currentValue, update)
  }

  return { handleTextareaPaste, handlePasteComplete }
}
