<template>
  <div class="bg-base-200 rounded-b-lg p-5 shadow-lg animate-dropdown relative">
    <ul class="daisy-menu mt-2 min-w-64 p-0">
      <li>
        <PopButton
          btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm h-auto min-h-0 w-full justify-start gap-2 py-2 font-normal"
          title="Export..."
        >
          <template #button_face>
            <Upload class="w-5 h-5 shrink-0" aria-hidden="true" />
            <span>Export...</span>
          </template>
          <template #default="{ closer }">
            <NoteExportForm :note="note" @close-dialog="closer" />
          </template>
        </PopButton>
      </li>

      <li>
        <PopButton
          btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm h-auto min-h-0 w-full justify-start gap-2 py-2 font-normal"
          title="Questions for the note"
        >
          <template #button_face>
            <MessageCircleQuestion
              class="w-5 h-5 shrink-0"
              aria-hidden="true"
            />
            <span>Questions for the note</span>
          </template>
          <template #default>
            <Questions v-bind="{ note }" />
          </template>
        </PopButton>
      </li>

      <li>
        <button
          class="daisy-btn daisy-btn-ghost daisy-btn-sm h-auto min-h-0 w-full justify-start gap-2 py-2 font-normal"
          title="Assimilation settings"
          @click="assimilateNote"
        >
          <CircleCheck class="w-5 h-5 shrink-0" aria-hidden="true" />
          <span>Assimilation settings</span>
        </button>
      </li>

      <li>
        <button
          class="daisy-btn daisy-btn-ghost daisy-btn-sm h-auto min-h-0 w-full justify-start gap-2 py-2 font-normal"
          title="Delete note"
          @click="deleteNote"
        >
          <Trash2 class="w-5 h-5 shrink-0" aria-hidden="true" />
          <span>Delete note</span>
        </button>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import PopButton from "../../commons/Popups/PopButton.vue"
import Questions from "../Questions.vue"
import {
  CircleCheck,
  MessageCircleQuestion,
  Trash2,
  Upload,
} from "lucide-vue-next"
import NoteExportForm from "../core/NoteExportForm.vue"
import { useRouter } from "vue-router"
import usePopups from "../../commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { NoteDeleteReferenceHandling } from "@/store/StoredApiCollection"

const { note } = defineProps<{
  note: Note
}>()

const emit = defineEmits<{
  (e: "close-dialog"): void
}>()

const router = useRouter()
const { popups } = usePopups()
const storageAccessor = useStorageAccessor()

const closeDialog = () => {
  emit("close-dialog")
}

const assimilateNote = () => {
  router.push({ name: "assimilateSingleNote", params: { noteId: note.id } })
  closeDialog()
}

const noteHasReferences = () =>
  (storageAccessor.value.refOfNoteRealm(note.id).value?.references?.length ??
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
    .deleteNote(router, note.id, referenceHandling)
}
</script>

<style scoped>
@keyframes dropDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-dropdown {
  animation: dropDown 0.3s ease-out;
  transform-origin: top;
}
</style>
