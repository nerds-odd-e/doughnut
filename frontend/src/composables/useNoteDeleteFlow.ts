import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { runWithBlockingApiLoading } from "@/managedApi/clientSetup"
import type {
  NoteDeleteOptions,
  NoteDeleteReferenceHandling,
} from "@/store/StoredApiCollection"
import { qualifyRelationNoteForReduceOnDelete } from "@/utils/relationNoteReduceOnDelete"
import { quotedNoteLabel } from "@/utils/quotedNoteLabel"
import { toValue, type MaybeRefOrGetter } from "vue"
import { useRouter } from "vue-router"

const REDUCE_DELETE_LOADING_MESSAGE = "Reducing to source property..."
const DELETE_LOADING_MESSAGE = "Deleting note..."

function deleteLoadingMessageFor(
  referenceHandling: NoteDeleteReferenceHandling
): string {
  return referenceHandling === "REDUCE_TO_SOURCE_PROPERTY"
    ? REDUCE_DELETE_LOADING_MESSAGE
    : DELETE_LOADING_MESSAGE
}

export function useNoteDeleteFlow(
  noteId: MaybeRefOrGetter<number>,
  noteTitle: MaybeRefOrGetter<string>
) {
  const router = useRouter()
  const { popups } = usePopups()
  const storageAccessor = useStorageAccessor()

  const noteRealm = () =>
    storageAccessor.value.refOfNoteRealm(toValue(noteId)).value

  const noteHasReferences = () => (noteRealm()?.references?.length ?? 0) > 0

  const chooseDeleteReferenceHandling =
    async (): Promise<NoteDeleteOptions | null> => {
      const id = toValue(noteId)
      const title = toValue(noteTitle)
      const label = quotedNoteLabel(title, id)
      const reduceQualification = qualifyRelationNoteForReduceOnDelete(
        storageAccessor.value.refOfNoteRealm(id).value
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
            sourceNoteId: reduceQualification.sourceNoteId,
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

    await runWithBlockingApiLoading(async () => {
      await storageAccessor.value
        .storedApi()
        .deleteNote(router, toValue(noteId), deleteChoice)
    }, deleteLoadingMessageFor(deleteChoice.referenceHandling))
  }

  return { deleteNote }
}
