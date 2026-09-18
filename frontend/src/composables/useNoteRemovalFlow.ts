import usePopups from "@/components/commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { runWithBlockingApiLoading } from "@/managedApi/clientSetup"
import type {
  NoteTrashOptions,
  NoteTrashReferenceHandling,
} from "@/store/StoredApiCollection"
import { isRelationshipNote } from "@/utils/relationNoteReduceOnTrash"
import { isNoteRealmInTrash } from "@/utils/folderTrash"
import { quotedNoteLabel } from "@/utils/quotedNoteLabel"
import { computed, toValue, type MaybeRefOrGetter } from "vue"
import { useRouter } from "vue-router"
import {
  closeAndFlushNoteContentMutations,
  reopenNoteContentMutations,
} from "@/composables/noteContentMutationBarrier"

const REDUCE_TO_PROPERTY_LOADING_MESSAGE = "Reducing to source property..."
const TRASH_LOADING_MESSAGE = "Trashing note..."
const PERMANENT_DELETE_LOADING_MESSAGE = "Permanently deleting note..."

const permanentDeleteWarning = (label: string) =>
  `Permanently delete ${label}? Its learning history, questions, conversations and images are deleted too, and this cannot be undone. Earlier Git history of this notebook still contains its text.`

type TrashFlowChoice =
  | { action: "reduce" }
  | { action: "trash"; options: NoteTrashOptions }

function loadingMessageFor(flowChoice: TrashFlowChoice): string {
  return flowChoice.action === "reduce"
    ? REDUCE_TO_PROPERTY_LOADING_MESSAGE
    : TRASH_LOADING_MESSAGE
}

export function useNoteRemovalFlow(
  noteId: MaybeRefOrGetter<number>,
  noteTitle: MaybeRefOrGetter<string>
) {
  const router = useRouter()
  const { popups } = usePopups()
  const storageAccessor = useStorageAccessor()

  const noteRealm = () =>
    storageAccessor.value.refOfNoteRealm(toValue(noteId)).value

  const noteHasReferences = () => (noteRealm()?.references?.length ?? 0) > 0

  const noteIsTrashed = computed(() => isNoteRealmInTrash(noteRealm()))

  const permanentlyDeleteNote = async () => {
    const id = toValue(noteId)
    const label = quotedNoteLabel(toValue(noteTitle), id)
    if (!(await popups.confirm(permanentDeleteWarning(label)))) return

    await runWithBlockingApiLoading(async () => {
      if (!(await closeAndFlushNoteContentMutations(id))) return
      await storageAccessor.value.storedApi().permanentlyDeleteNote(router, id)
    }, PERMANENT_DELETE_LOADING_MESSAGE)
  }

  const chooseTrashReferenceHandling =
    async (): Promise<TrashFlowChoice | null> => {
      const id = toValue(noteId)
      const title = toValue(noteTitle)
      const label = quotedNoteLabel(title, id)
      const noteIsRelationship = isRelationshipNote(
        storageAccessor.value.refOfNoteRealm(id).value
      )
      if (noteIsRelationship) {
        const choice = await popups.options(
          `${label} is a relationship. What should happen?`,
          [
            {
              label:
                "Reduce to a property of the source (permanently deletes this relationship note; cannot be undone)",
              value: "REDUCE",
            },
            {
              label: `Trash ${label}`,
              value: "LEAVE_DEAD_LINKS",
            },
          ]
        )
        if (!choice) return null
        if (choice === "REDUCE") {
          return { action: "reduce" }
        }
        return {
          action: "trash",
          options: { referenceHandling: "LEAVE_DEAD_LINKS" },
        }
      }

      if (!noteHasReferences()) {
        return (await popups.confirm(`Confirm to trash ${label}?`))
          ? {
              action: "trash",
              options: { referenceHandling: "LEAVE_DEAD_LINKS" },
            }
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
      return referenceHandling
        ? { action: "trash", options: { referenceHandling } }
        : null
    }

  const trashNote = async () => {
    const flowChoice = await chooseTrashReferenceHandling()
    if (!flowChoice) return

    await runWithBlockingApiLoading(async () => {
      const id = toValue(noteId)
      if (!(await closeAndFlushNoteContentMutations(id))) return
      const storage = storageAccessor.value
      try {
        if (flowChoice.action === "reduce") {
          await storage
            .storedApi()
            .reduceRelationNoteToSourceProperty(router, id)
        } else {
          await storage.storedApi().trashNote(router, id, flowChoice.options)
        }
      } finally {
        if (storage.refOfNoteRealm(id).value) {
          reopenNoteContentMutations(id)
        }
      }
    }, loadingMessageFor(flowChoice))
  }

  return { trashNote, permanentlyDeleteNote, noteIsTrashed }
}
