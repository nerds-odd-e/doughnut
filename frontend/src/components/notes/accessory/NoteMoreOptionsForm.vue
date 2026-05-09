<template>
  <div class="daisy-bg-base-200 daisy-rounded-b-lg daisy-p-5 daisy-shadow-lg animate-dropdown daisy-relative">
    <button class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-btn-circle daisy-absolute daisy-top-2 daisy-right-2" @click="closeDialog" title="Close">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
        <path d="M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z"/>
      </svg>
    </button>
    <div class="daisy-btn-group daisy-btn-group-horizontal daisy-justify-end daisy-mt-2">
      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        title="Edit Note Image"
      >
        <template #button_face>
          <ImageIcon class="daisy-w-6 daisy-h-6" />
        </template>
        <template #default="{ closer }">
          <NoteEditImageForm
            v-bind="{ noteId: note.id }"
            @close-dialog="noteAccessoriesUpdated(closer, $event)"
          />
        </template>
      </PopButton>

      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        title="Export..."
      >
        <template #button_face>
          <Upload class="daisy-w-6 daisy-h-6" />
        </template>
        <template #default="{ closer }">
          <NoteExportForm :note="note" @close-dialog="closer" />
        </template>
      </PopButton>

      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        title="Questions for the note"
      >
        <template #button_face>
          <MessageCircleQuestion class="daisy-w-6 daisy-h-6" />
        </template>
        <template #default>
          <Questions v-bind="{ note }" />
        </template>
      </PopButton>

      <button
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        title="Assimilation settings"
        @click="assimilateNote"
      >
        <CircleCheck class="daisy-w-6 daisy-h-6" />
      </button>

      <button
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        title="Delete note"
        @click="deleteNote"
      >
        <Trash2 class="daisy-w-6 daisy-h-6" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Note, NoteAccessory } from "@generated/doughnut-backend-api"
import PopButton from "../../commons/Popups/PopButton.vue"
import Questions from "../Questions.vue"
import {
  CircleCheck,
  Image as ImageIcon,
  MessageCircleQuestion,
  Trash2,
  Upload,
} from "lucide-vue-next"
import NoteEditImageForm from "./NoteEditImageForm.vue"
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
  (e: "note-accessory-updated", na: NoteAccessory): void
}>()

const router = useRouter()
const { popups } = usePopups()
const storageAccessor = useStorageAccessor()

const closeDialog = () => {
  emit("close-dialog")
}

const noteAccessoriesUpdated = (closer: () => void, na: NoteAccessory) => {
  if (na) {
    emit("note-accessory-updated", na)
  }
  closer()
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
