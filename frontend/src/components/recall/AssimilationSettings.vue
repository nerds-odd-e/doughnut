<template>
  <footer
    class="assimilation-settings-bar pointer-events-none z-40"
    aria-label="Assimilation settings"
  >
    <div
      class="assimilation-settings-inner pointer-events-auto w-full px-4 pb-[max(0.75rem,env(safe-area-inset-bottom))] pt-0"
    >
      <div
        class="daisy-card bg-base-100 border border-base-300 shadow-xl"
      >
        <div class="daisy-card-body gap-0 p-4">
          <h2 class="daisy-card-title mb-3 text-base font-semibold">
            Assimilation settings
          </h2>
          <div
            class="assimilation-settings-scroll max-h-[min(40vh,22rem)] overflow-y-auto pr-1"
          >
            <NoteInfoBar
              :note-id="note.id"
              @level-changed="emit('levelChanged', $event)"
              @remember-spelling-changed="emit('rememberSpellingChanged', $event)"
              @note-recall-info-loaded="emit('noteRecallInfoLoaded', $event)"
            />
          </div>
          <div class="daisy-divider my-4" />
          <div
            class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
          >
            <button
              v-if="(note.content ?? '').trim()"
              type="button"
              data-test="open-refine-note-modal"
              class="daisy-btn daisy-btn-neutral shrink-0"
              @click="showRefineNoteModal = true"
            >
              Refine note
            </button>
            <div
              class="flex flex-wrap items-stretch justify-end gap-2 sm:flex-1"
            >
              <AssimilationButtons
                :disabled="!noteInfoLoaded"
                :keep-for-recall-disabled="keepForRecallDisabled"
                @assimilate="emit('assimilate', $event)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </footer>
  <Teleport to="body">
    <dialog
      v-if="(note.content ?? '').trim()"
      ref="refineNoteDialogRef"
      class="daisy-modal"
      :class="{ 'daisy-modal-open': showRefineNoteModal }"
      data-test="refine-note-modal"
      @close="closeRefineNoteModal"
    >
      <div
        class="daisy-modal-box max-w-4xl max-h-[90vh] overflow-y-auto"
      >
        <h3
          v-if="showRefineNoteModal"
          class="font-bold text-lg mb-3"
        >
          Refine note
        </h3>
        <NoteRefinement
          v-if="showRefineNoteModal"
          :key="note.id"
          :note="note"
          @content-updated="emit('refinementContentUpdated')"
          @understanding-points-ignored="closeRefineNoteModal"
        />
        <div v-if="showRefineNoteModal" class="daisy-modal-action mt-4">
          <button
            type="button"
            class="daisy-btn"
            data-test="close-refine-note-modal"
            @click="closeRefineNoteModal"
          >
            Close
          </button>
        </div>
      </div>
      <form method="dialog" class="daisy-modal-backdrop">
        <button type="button" @click="closeRefineNoteModal">close</button>
      </form>
    </dialog>
  </Teleport>
</template>

<script setup lang="ts">
import type { Note, NoteRecallInfo } from "@generated/doughnut-backend-api"
import NoteInfoBar from "../notes/NoteInfoBar.vue"
import AssimilationButtons from "./AssimilationButtons.vue"
import NoteRefinement from "./NoteRefinement.vue"
import { useDaisyDialog } from "@/composables/useDaisyDialog"
import { ref, watch } from "vue"

const { note, noteInfoLoaded, keepForRecallDisabled } = defineProps<{
  note: Note
  noteInfoLoaded: boolean
  keepForRecallDisabled: boolean
}>()

const emit = defineEmits<{
  (e: "levelChanged", value: unknown): void
  (e: "rememberSpellingChanged", value: boolean): void
  (e: "noteRecallInfoLoaded", value: NoteRecallInfo): void
  (e: "assimilate", skipMemoryTracking: boolean): void
  (e: "refinementContentUpdated"): void
}>()

const showRefineNoteModal = ref(false)
const refineNoteDialogRef = ref<HTMLDialogElement | null>(null)
useDaisyDialog(showRefineNoteModal, refineNoteDialogRef)

watch(
  () => note.id,
  () => {
    showRefineNoteModal.value = false
  }
)

const closeRefineNoteModal = () => {
  showRefineNoteModal.value = false
}
</script>

<style scoped lang="scss">
@use "@/assets/menu-variables.scss" as *;

/* Short viewports: stay in document flow after the note. Taller: dock to bottom. */
.assimilation-settings-bar {
  position: relative;
  width: 100%;
  margin-top: 1rem;
}

@media (min-height: $assimilation-dock-min-height) {
  .assimilation-settings-bar {
    position: fixed;
    right: 0;
    bottom: 0;
    left: $main-menu-width;
    width: auto;
    margin-top: 0;
  }
}

@media (min-height: $assimilation-dock-min-height) and (max-width: 1024px) {
  .assimilation-settings-bar {
    left: 0;
  }
}
</style>
