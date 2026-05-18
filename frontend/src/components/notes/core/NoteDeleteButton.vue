<template>
  <button title="Delete note" @click="deleteNote">
    <Trash2 class="w-6 h-6" />Delete note
  </button>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router"
import usePopups from "../../commons/Popups/usePopups"
import { Trash2 } from "@lucide/vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { NoteDeleteReferenceHandling } from "@/store/StoredApiCollection"

const router = useRouter()
const { popups } = usePopups()
const storageAccessor = useStorageAccessor()

const props = defineProps({
  noteId: { type: Number, required: true },
})

const noteHasReferences = () =>
  (storageAccessor.value.refOfNoteRealm(props.noteId).value?.references
    ?.length ?? 0) > 0

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
    .deleteNote(router, props.noteId, referenceHandling)
}
</script>
