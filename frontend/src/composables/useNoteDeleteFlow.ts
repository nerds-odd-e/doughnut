import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { NoteDeleteReferenceHandling } from "@/store/StoredApiCollection"
import { useRouter } from "vue-router"

export function useNoteDeleteFlow(noteId: number) {
  const router = useRouter()
  const { popups } = usePopups()
  const storageAccessor = useStorageAccessor()

  const noteHasReferences = () =>
    (storageAccessor.value.refOfNoteRealm(noteId).value?.references?.length ??
      0) > 0

  const chooseDeleteReferenceHandling =
    async (): Promise<NoteDeleteReferenceHandling | null> => {
      if (!noteHasReferences()) {
        return (await popups.confirm(`Confirm to delete this note?`))
          ? "LEAVE_DEAD_LINKS"
          : null
      }
      return (await popups.options(
        "This note has references. How should they be handled?",
        [
          {
            label:
              "Remove from properties of references (undo will not recover the removed property)",
            value: "REMOVE_FROM_PROPERTIES",
          },
          {
            label: "Leave all references as dead link",
            value: "LEAVE_DEAD_LINKS",
          },
        ]
      )) as NoteDeleteReferenceHandling | null
    }

  const deleteNote = async () => {
    const referenceHandling = await chooseDeleteReferenceHandling()
    if (!referenceHandling) return
    await storageAccessor.value
      .storedApi()
      .deleteNote(router, noteId, referenceHandling)
  }

  return { deleteNote }
}
