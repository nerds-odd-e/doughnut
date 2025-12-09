<template>
  <div class="daisy-bg-base-200 daisy-rounded-b-lg daisy-p-5 daisy-shadow-lg animate-dropdown daisy-relative">
    <button class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-btn-circle daisy-absolute daisy-top-2 daisy-right-2" @click="closeDialog" title="Close">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
        <path d="M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z"/>
      </svg>
    </button>
    <div class="daisy-flex daisy-flex-col daisy-gap-2">
      <div v-if="noteInfo?.note" class="daisy-mb-4">
        <NoteInfoComponent :note-info="noteInfo" />
      </div>
      <div class="daisy-mb-4">
        <Select
          id="note-type-edit"
          v-model="localNoteType"
          :options="noteTypeOptions"
          scope-name="note"
          field="noteType"
          @update:model-value="updateNoteType"
        ></Select>
      </div>
    </div>
    <div class="daisy-divider daisy-my-2"></div>
    <div class="daisy-btn-group daisy-btn-group-horizontal daisy-justify-end">
      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        title="Generate Image with DALL-E"
      >
        <template #button_face>
          <SvgRobot />
        </template>
        <template #default>
          <AIGenerateImageDialog v-bind="{ note }" />
        </template>
      </PopButton>

      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        title="Edit Note Image"
      >
        <template #button_face>
          <SvgImage />
        </template>
        <template #default="{ closer }">
          <NoteEditImageDialog
            v-bind="{ noteId: note.id }"
            @close-dialog="noteAccessoriesUpdated(closer, $event)"
          />
        </template>
      </PopButton>

      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        title="Edit Note URL"
      >
        <template #button_face>
          <SvgUrlIndicator />
        </template>
        <template #default="{ closer }">
          <NoteEditUrlDialog
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
          <SvgExport />
        </template>
        <template #default="{ closer }">
          <NoteExportDialog :note="note" @close-dialog="closer" />
        </template>
      </PopButton>

      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        title="Questions for the note"
      >
        <template #button_face>
          <SvgAssessment />
        </template>
        <template #default>
          <Questions v-bind="{ note }" />
        </template>
      </PopButton>

      <button
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        title="Delete note"
        @click="deleteNote"
      >
        <SvgRemove />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import type { NoteAccessory } from "@generated/backend"
import PopButton from "../../commons/Popups/PopButton.vue"
import AIGenerateImageDialog from "../AIGenerateImageDialog.vue"
import Questions from "../Questions.vue"
import SvgUrlIndicator from "../../svgs/SvgUrlIndicator.vue"
import NoteEditImageDialog from "./NoteEditImageDialog.vue"
import NoteEditUrlDialog from "./NoteEditUrlDialog.vue"
import SvgExport from "../../svgs/SvgExport.vue"
import NoteExportDialog from "../core/NoteExportDialog.vue"
import SvgAssessment from "../../svgs/SvgAssessment.vue"
import SvgRemove from "../../svgs/SvgRemove.vue"
import SvgRobot from "../../svgs/SvgRobot.vue"
import SvgImage from "../../svgs/SvgImage.vue"
import { useRouter } from "vue-router"
import usePopups from "../../commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { NoteInfo } from "@generated/backend"
import { NoteController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { ref, onMounted } from "vue"
import NoteInfoComponent from "../NoteInfoComponent.vue"
import Select from "../../form/Select.vue"

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

const noteInfo = ref<NoteInfo | undefined>(undefined)

const noteTypeOptions: Array<
  "unassigned" | "concept" | "category" | "vocab" | "journal"
> = ["unassigned", "concept", "category", "vocab", "journal"]

const localNoteType = ref<string>(note.noteType || "unassigned")

const updateNoteType = async (newType: string) => {
  const previousValue = localNoteType.value
  localNoteType.value = newType
  const { data: updatedNote, error } = await apiCallWithLoading(() =>
    NoteController.updateNoteType({
      path: { note: note.id },
      body: newType as
        | "concept"
        | "category"
        | "vocab"
        | "journal"
        | "unassigned",
    })
  )
  if (error) {
    localNoteType.value = previousValue
  } else if (updatedNote) {
    await fetchNoteInfo()
  }
}

const fetchNoteInfo = async () => {
  const { data: noteInfoData, error } = await NoteController.getNoteInfo({
    path: { note: note.id },
  })
  if (!error && noteInfoData) {
    noteInfo.value = noteInfoData
    const fetchedNoteType = noteInfoData.note?.note?.noteType
    if (fetchedNoteType) {
      localNoteType.value = fetchedNoteType
    } else {
      localNoteType.value = "unassigned"
    }
  }
}

onMounted(() => {
  fetchNoteInfo()
})

const closeDialog = () => {
  emit("close-dialog")
}

const noteAccessoriesUpdated = (closer: () => void, na: NoteAccessory) => {
  if (na) {
    emit("note-accessory-updated", na)
  }
  closer()
}

const deleteNote = async () => {
  if (await popups.confirm(`Confirm to delete this note?`)) {
    await storageAccessor.value.storedApi().deleteNote(router, note.id)
  }
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

