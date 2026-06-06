import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { NoteDeleteReferenceHandling } from "@/store/StoredApiCollection"
import { qualifyRelationNoteForReduceOnDelete } from "@/utils/relationNoteReduceOnDelete"
import { quotedNoteLabel } from "@/utils/quotedNoteLabel"
import { useRouter } from "vue-router"

export function useNoteDeleteFlow(noteId: number, noteTitle: string) {
  const router = useRouter()
  const { popups } = usePopups()
  const storageAccessor = useStorageAccessor()

  const noteRealm = () => storageAccessor.value.refOfNoteRealm(noteId).value

  const noteHasReferences = () => (noteRealm()?.references?.length ?? 0) > 0

  const chooseDeleteReferenceHandling = async (): Promise<{
    referenceHandling: NoteDeleteReferenceHandling
    sourcePropertyKey?: string
  } | null> => {
    const label = quotedNoteLabel(noteTitle, noteId)
    const reduceQualification = qualifyRelationNoteForReduceOnDelete(
      noteRealm()
    )
    if (reduceQualification) {
      const choice = await popups.options(
        `${label} is a relationship. What should happen?`,
        [
          {
            label: "Reduce to a property of the source",
            value: "REDUCE_TO_SOURCE_PROPERTY",
          },
          {
            label: `Delete ${label}`,
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
      return (await popups.confirm(`Confirm to delete ${label}?`))
        ? { referenceHandling: "LEAVE_DEAD_LINKS" }
        : null
    }
    const referenceHandling = (await popups.options(
      `${label} has references. How should they be handled?`,
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
