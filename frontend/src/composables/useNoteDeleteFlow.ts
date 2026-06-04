import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { NoteDeleteReferenceHandling } from "@/store/StoredApiCollection"
import { qualifyRelationNoteForReduceOnDelete } from "@/utils/relationNoteReduceOnDelete"
import { useRouter } from "vue-router"

export function useNoteDeleteFlow(noteId: number) {
  const router = useRouter()
  const { popups } = usePopups()
  const storageAccessor = useStorageAccessor()

  const noteRealm = () => storageAccessor.value.refOfNoteRealm(noteId).value

  const noteHasReferences = () => (noteRealm()?.references?.length ?? 0) > 0

  const chooseDeleteReferenceHandling = async (): Promise<{
    referenceHandling: NoteDeleteReferenceHandling
    sourcePropertyKey?: string
  } | null> => {
    const reduceQualification = qualifyRelationNoteForReduceOnDelete(
      noteRealm()
    )
    if (reduceQualification) {
      const choice = await popups.options(
        "This note is a relationship. What should happen?",
        [
          {
            label: "Reduce to a property of the source",
            value: "REDUCE_TO_SOURCE_PROPERTY",
          },
          {
            label: "Delete this note",
            value: "LEAVE_DEAD_LINKS",
          },
        ]
      )
      if (!choice) return null
      if (choice === "REDUCE_TO_SOURCE_PROPERTY") {
        return {
          referenceHandling: "REDUCE_TO_SOURCE_PROPERTY",
          sourcePropertyKey: reduceQualification.sourcePropertyKey,
        }
      }
      return { referenceHandling: "LEAVE_DEAD_LINKS" }
    }

    if (!noteHasReferences()) {
      return (await popups.confirm(`Confirm to delete this note?`))
        ? { referenceHandling: "LEAVE_DEAD_LINKS" }
        : null
    }
    const referenceHandling = (await popups.options(
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
    return referenceHandling ? { referenceHandling } : null
  }

  const deleteNote = async () => {
    const deleteChoice = await chooseDeleteReferenceHandling()
    if (!deleteChoice) return
    await storageAccessor.value
      .storedApi()
      .deleteNote(
        router,
        noteId,
        deleteChoice.referenceHandling,
        deleteChoice.sourcePropertyKey
      )
  }

  return { deleteNote }
}
