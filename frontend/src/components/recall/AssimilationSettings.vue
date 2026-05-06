<template>
  <footer
    class="assimilation-settings-bar daisy-pointer-events-none daisy-z-40"
    aria-label="Assimilation settings"
  >
    <div
      class="assimilation-settings-inner daisy-pointer-events-auto daisy-w-full daisy-px-4 daisy-pb-[max(0.75rem,env(safe-area-inset-bottom))] daisy-pt-0"
    >
      <div
        class="daisy-card daisy-bg-base-100 daisy-border daisy-border-base-300 daisy-shadow-xl"
      >
        <div class="daisy-card-body daisy-gap-0 daisy-p-4">
          <h2 class="daisy-card-title daisy-mb-3 daisy-text-base daisy-font-semibold">
            Assimilation settings
          </h2>
          <div
            class="assimilation-settings-scroll daisy-max-h-[min(40vh,22rem)] daisy-overflow-y-auto daisy-pr-1"
          >
            <NoteInfoBar
              :note-id="note.id"
              @level-changed="emit('levelChanged', $event)"
              @remember-spelling-changed="emit('rememberSpellingChanged', $event)"
              @note-recall-info-loaded="emit('noteRecallInfoLoaded', $event)"
            />
          </div>
          <div class="daisy-divider daisy-my-4" />
          <div
            class="daisy-flex daisy-flex-col daisy-gap-3 sm:daisy-flex-row sm:daisy-items-center sm:daisy-justify-between"
          >
            <button
              v-if="(note.details ?? '').trim()"
              type="button"
              data-test="open-refine-note-modal"
              class="daisy-btn daisy-btn-neutral daisy-shrink-0"
              @click="showRefineNoteModal = true"
            >
              Refine note
            </button>
            <div
              class="daisy-flex daisy-flex-wrap daisy-items-stretch daisy-justify-end daisy-gap-2 sm:daisy-flex-1"
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
      v-if="(note.details ?? '').trim()"
      class="daisy-modal"
      :class="{ 'daisy-modal-open': showRefineNoteModal }"
      data-test="refine-note-modal"
    >
      <div
        class="daisy-modal-box daisy-max-w-4xl daisy-max-h-[90vh] daisy-overflow-y-auto"
      >
        <h3
          v-if="showRefineNoteModal"
          class="daisy-font-bold daisy-text-lg daisy-mb-3"
        >
          Refine note
        </h3>
        <NoteRefinement
          v-if="showRefineNoteModal"
          :key="note.id"
          :note="note"
          @details-updated="emit('refinementDetailsUpdated')"
          @understanding-points-ignored="closeRefineNoteModal"
        />
        <div v-if="showRefineNoteModal" class="daisy-modal-action daisy-mt-4">
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
  (e: "refinementDetailsUpdated"): void
}>()

const showRefineNoteModal = ref(false)

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

@media (min-height: $assimilation-dock-min-height) and (max-width: theme("screens.lg")) {
  .assimilation-settings-bar {
    left: 0;
  }
}
</style>
