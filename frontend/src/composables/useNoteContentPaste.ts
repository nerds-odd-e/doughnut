import { nextTick, type Ref } from "vue"
import type { NoteRealm } from "@generated/donut-backend-api"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import type TextArea from "@/components/form/TextArea.vue"
import { usePasteWithLinkImageOptions } from "@/composables/usePasteWithLinkImageOptions"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { WikiLinkNoteIdentity } from "@/utils/buildWikiLinkText"
import { convertPastedNotePropertyLinksInNoteContent } from "@/utils/convertPastedNotePropertyLinks"

type NoteContentUpdate = (noteId: number, newValue: string) => void

function wikiLinkNoteIdentityFromRealm(realm: NoteRealm): WikiLinkNoteIdentity {
  return {
    noteTopology: { title: realm.note.noteTopology.title },
    notebookId: realm.notebookRealm.notebook.id,
    notebookName: realm.notebookRealm.notebook.name,
  }
}

export function useNoteContentPaste(options: {
  noteId: () => number
  asMarkdown: () => boolean
  textareaRef: Ref<InstanceType<typeof TextArea> | null>
}) {
  const storageAccessor = useStorageAccessor()
  const { htmlToMarkdown, processContentAfterPaste } =
    usePasteWithLinkImageOptions()

  async function resolvePastedNoteIdentity(
    noteId: number
  ): Promise<WikiLinkNoteIdentity | undefined> {
    const cached = storageAccessor.value
      .storedApi()
      .getNoteRealmRef(noteId).value
    if (cached) {
      return wikiLinkNoteIdentityFromRealm(cached)
    }
    const { data, error } = await NoteController.showNote({
      path: { note: noteId },
    })
    if (error || !data) {
      return undefined
    }
    return wikiLinkNoteIdentityFromRealm(data)
  }

  function convertPastedPropertyLinks(content: string): Promise<string> {
    const sourceNotebookId = storageAccessor.value
      .storedApi()
      .getNoteRealmRef(options.noteId()).value?.notebookRealm.notebook.id
    return convertPastedNotePropertyLinksInNoteContent(content, {
      sourceNotebookId,
      resolveNote: resolvePastedNoteIdentity,
    })
  }

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
    const spliced =
      (currentValue || "").slice(0, start) +
      markdown +
      (currentValue || "").slice(end)
    const newValue = await convertPastedPropertyLinks(spliced)

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
    const converted = await convertPastedPropertyLinks(currentValue)
    if (converted !== currentValue) {
      update(options.noteId(), converted)
    }
    await offerToRemoveLinksAndImages(converted, update)
  }

  return { handleTextareaPaste, handlePasteComplete }
}
