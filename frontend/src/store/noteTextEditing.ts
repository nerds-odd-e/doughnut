import type {
  DictatedText,
  NoteContentCompletion,
} from "@generated/donut-backend-api"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import { updateTextContentRequest } from "./noteRequests"
import type { NoteStore, TitleRenameReferenceHandling } from "./noteStore"

/** Text mutations share persistence and undo while retaining distinct append/replacement semantics. */
export default class NoteTextEditing {
  constructor(private store: NoteStore) {}

  private async updateTextContentWithoutUndo(
    noteId: Donut.ID,
    field: "edit title" | "edit content",
    content: string,
    titleReferenceHandling?: TitleRenameReferenceHandling
  ) {
    const realm = this.store.refreshNoteRealm(
      await updateTextContentRequest(
        noteId,
        field,
        content,
        titleReferenceHandling
      )
    )
    if (field === "edit title") {
      refreshSidebarStructuralListings()
    }
    return realm
  }

  async updateTextField(
    noteId: Donut.ID,
    field: "edit title" | "edit content",
    value: string,
    options?: { titleReferenceHandling?: TitleRenameReferenceHandling }
  ) {
    const currentNote = this.store.refOfNoteRealm(noteId).value?.note
    if (currentNote) {
      const old =
        field === "edit title"
          ? currentNote.noteTopology.title
          : (currentNote.content ?? "")
      if (old === value) {
        return
      }
      this.store.noteUndo.addEditingToUndoHistory(noteId, field, old)
    }
    await this.updateTextContentWithoutUndo(
      noteId,
      field,
      value,
      field === "edit title" ? options?.titleReferenceHandling : undefined
    )
  }

  async setNoteContentWithoutUndo(noteId: Donut.ID, content: string) {
    await this.updateTextContentWithoutUndo(noteId, "edit content", content)
  }

  async completeContent(noteId: Donut.ID, value?: NoteContentCompletion) {
    if (!value || !value.content) return

    await this.store.getOrLoadNoteRealm(noteId)

    await this.updateTextField(noteId, "edit content", value.content)
  }

  async appendDictatedText(noteId: Donut.ID, value?: DictatedText) {
    if (!value?.dictatedText) return

    const realm = await this.store.getOrLoadNoteRealm(noteId)
    await this.updateTextField(
      noteId,
      "edit content",
      (realm.note.content ?? "") + value.dictatedText
    )
  }
}
