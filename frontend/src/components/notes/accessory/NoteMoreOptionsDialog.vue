<template>
  <div class="daisy-bg-base-200 daisy-rounded-b-lg daisy-p-5 daisy-shadow-lg animate-dropdown daisy-relative">
    <button class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-btn-circle daisy-absolute daisy-top-2 daisy-right-2" @click="closeDialog" title="Close">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
        <path d="M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z"/>
      </svg>
    </button>
    <div class="daisy-flex daisy-flex-col daisy-gap-2">
      <div v-if="noteInfo?.note" class="daisy-mb-4">
        <h6>Recall Settings</h6>
        <RecallSettingForm
          v-bind="{ noteId: note.id, recallSetting }"
          @level-changed="handleLevelChanged"
        />
        <h6 v-if="memoryTrackers.length" class="daisy-mt-4">Memory Trackers</h6>
        <table v-if="memoryTrackers.length" class="daisy-table daisy-table-bordered daisy-mt-2">
          <thead>
            <tr>
              <th>Type</th>
              <th>Repetition Count</th>
              <th>Forgetting Curve Index</th>
              <th>Next Recall</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="memoryTracker in memoryTrackers"
              :key="memoryTracker.id"
              class="clickable-row"
              @click="navigateToMemoryTracker(memoryTracker.id)"
            >
              <NoteInfoMemoryTracker
                :model-value="memoryTracker"
                @update:model-value="updateMemoryTracker($event)"
              />
            </tr>
          </tbody>
        </table>
      </div>

      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-w-full daisy-justify-start"
        title="Edit Note Image"
      >
        <template #button_face>
          <SvgImage />
          <span class="ms-2">Edit Note Image</span>
        </template>
        <template #default="{ closer }">
          <NoteEditImageDialog
            v-bind="{ noteId: note.id }"
            @close-dialog="noteAccessoriesUpdated(closer, $event)"
          />
        </template>
      </PopButton>

      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-w-full daisy-justify-start"
        title="Edit Note URL"
      >
        <template #button_face>
          <SvgUrlIndicator />
          <span class="ms-2">Edit Note URL</span>
        </template>
        <template #default="{ closer }">
          <NoteEditUrlDialog
            v-bind="{ noteId: note.id }"
            @close-dialog="noteAccessoriesUpdated(closer, $event)"
          />
        </template>
      </PopButton>
    </div>
    <div class="daisy-divider daisy-my-2"></div>
    <div class="daisy-btn-group daisy-btn-group-horizontal daisy-justify-end">
      <PopButton
        btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        title="Generate Image with DALL-E"
      >
        <template #button_face>
          <SvgImage />
        </template>
        <template #default>
          <AIGenerateImageDialog v-bind="{ note }" />
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
import SvgImage from "../../svgs/SvgImage.vue"
import SvgUrlIndicator from "../../svgs/SvgUrlIndicator.vue"
import NoteEditImageDialog from "./NoteEditImageDialog.vue"
import NoteEditUrlDialog from "./NoteEditUrlDialog.vue"
import SvgExport from "../../svgs/SvgExport.vue"
import NoteExportDialog from "../core/NoteExportDialog.vue"
import SvgAssessment from "../../svgs/SvgAssessment.vue"
import SvgRemove from "../../svgs/SvgRemove.vue"
import { useRouter } from "vue-router"
import usePopups from "../../commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { NoteInfo, MemoryTracker } from "@generated/backend"
import { NoteController } from "@generated/backend/sdk.gen"
import { ref, computed, onMounted } from "vue"
import RecallSettingForm from "../../review/RecallSettingForm.vue"
import NoteInfoMemoryTracker from "../NoteInfoMemoryTracker.vue"

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
const memoryTrackers = ref<MemoryTracker[]>([])

const recallSetting = computed(() => noteInfo.value?.recallSetting)

const fetchNoteInfo = async () => {
  const { data: noteInfoData, error } = await NoteController.getNoteInfo({
    path: { note: note.id },
  })
  if (!error && noteInfoData) {
    noteInfo.value = noteInfoData
    memoryTrackers.value = noteInfoData.memoryTrackers ?? []
  }
}

onMounted(() => {
  fetchNoteInfo()
})

const handleLevelChanged = () => {
  // Handle level change if needed
}

const updateMemoryTracker = (newTracker: MemoryTracker) => {
  const index = memoryTrackers.value.findIndex((t) => t.id === newTracker.id)
  if (index !== -1) {
    memoryTrackers.value[index] = newTracker
  }
}

const navigateToMemoryTracker = (memoryTrackerId: number) => {
  router.push({
    name: "memoryTrackerShow",
    params: { memoryTrackerId },
  })
}

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

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background-color: rgba(0, 0, 0, 0.05);
}
</style>

