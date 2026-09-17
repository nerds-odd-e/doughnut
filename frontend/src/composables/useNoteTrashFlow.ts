import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { runWithBlockingApiLoading } from "@/managedApi/clientSetup"
import type {
  NoteTrashOptions,
  NoteTrashReferenceHandling,
} from "@/store/StoredApiCollection"
import { qualifyRelationNoteForReduceOnTrash } from "@/utils/relationNoteReduceOnTrash"
import { quotedNoteLabel } from "@/utils/quotedNoteLabel"
import { toValue, type MaybeRefOrGetter } from "vue"
import { useRouter } from "vue-router"
import {
  closeAndFlushNoteContentMutations,
  reopenNoteContentMutations,
} from "@/composables/noteContentMutationBarrier"

const REDUCE_TO_PROPERTY_LOADING_MESSAGE = "Reducing to source property..."
const TRASH_LOADING_MESSAGE = "Trashing note..."

function trashLoadingMessageFor(
  referenceHandling: NoteTrashReferenceHandling
): string {
  return referenceHandling === "REDUCE_TO_SOURCE_PROPERTY"
    ? REDUCE_TO_PROPERTY_LOADING_MESSAGE
    : TRASH_LOADING_MESSAGE
}

export function useNoteTrashFlow(
  noteId: MaybeRefOrGetter<number>,
  noteTitle: MaybeRefOrGetter<string>
) {
  const router = useRouter()
  const { popups } = usePopups()
  const storageAccessor = useStorageAccessor()

  const noteRealm = () =>
    storageAccessor.value.refOfNoteRealm(toValue(noteId)).value

  const noteHasReferences = () => (noteRealm()?.references?.length ?? 0) > 0

  const chooseTrashReferenceHandling =
    async (): Promise<NoteTrashOptions | null> => {
      const id = toValue(noteId)
      const title = toValue(noteTitle)
      const label = quotedNoteLabel(title, id)
      const reduceQualification = qualifyRelationNoteForReduceOnTrash(
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
              label: `Trash ${label}`,
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
        return (await popups.confirm(`Confirm to trash ${label}?`))
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
            label: "Leave all references as dead wiki links",
            value: "LEAVE_DEAD_LINKS",
          },
        ]
      )) as NoteTrashReferenceHandling | null
      return referenceHandling ? { referenceHandling } : null
    }

  const trashNote = async () => {
    const trashChoice = await chooseTrashReferenceHandling()
    if (!trashChoice) return

    await runWithBlockingApiLoading(async () => {
      const id = toValue(noteId)
      if (!(await closeAndFlushNoteContentMutations(id))) return
      const storage = storageAccessor.value
      try {
        await storage.storedApi().trashNote(router, id, trashChoice)
      } finally {
        if (storage.refOfNoteRealm(id).value) {
          reopenNoteContentMutations(id)
        }
      }
    }, trashLoadingMessageFor(trashChoice.referenceHandling))
  }

  return { trashNote }
}
