<template>
  <section
    aria-label="Assimilation settings"
    data-testid="assimilation-settings"
    class="flex flex-col gap-0"
  >
    <h2 class="text-base font-semibold gap-2 mb-3 flex items-center">
      Assimilation settings
      <AssimilationProgressSummary />
    </h2>
    <NoteInfoComponent
      v-if="noteRecallInfo"
      :note-recall-info="noteRecallInfo"
    />
    <div class="daisy-divider my-4" />
    <div
      class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
    >
      <button
        v-if="hasNoteContent"
        type="button"
        data-test="open-refine-note-modal"
        class="daisy-btn daisy-btn-neutral shrink-0"
        @click="showRefineNoteModal = true"
      >
        Refine note
      </button>
      <div class="flex flex-wrap items-stretch justify-end gap-2 sm:flex-1">
        <AssimilationButtons
          :disabled="!noteInfoLoaded"
          :assimilate-disabled="assimilateDisabled"
          :skipped-for-recall="isSkippedForRecall(noteRecallInfo)"
          :skipped-from-assimilation-sequence="
            isSkippedFromAssimilationSequence(noteRecallInfo)
          "
          :show-remove-from-recall="showRemoveFromRecall(noteRecallInfo)"
          :show-commissioned-option="showCommissionedOption"
          :show-spelling-option="showSpellingOption"
          @assimilate="emit('assimilate', {})"
          @skip="emit('skip', {})"
          @assimilate-as-commissioned="
            emit('assimilate', {
              assimilateAsCommissioned: true,
            })
          "
          @remember-spelling="
            emit('assimilate', {
              assimilateAsSpelling: true,
            })
          "
          @revive="emit('revive', {})"
          @return-to-sequence="emit('returnToSequence', {})"
          @remove-from-recall="emit('removeFromRecall', {})"
        />
      </div>
    </div>
  </section>
  <RefineNoteModal
    v-if="hasNoteContent"
    v-model:open="showRefineNoteModal"
    :note="note"
    @content-updated="emit('refinementContentUpdated')"
  />
</template>

<script setup lang="ts">
import type { Note } from "@generated/donut-backend-api"
import NoteInfoComponent from "../notes/NoteInfoComponent.vue"
import AssimilationButtons from "./AssimilationButtons.vue"
import AssimilationProgressSummary from "./AssimilationProgressSummary.vue"
import RefineNoteModal from "./RefineNoteModal.vue"
import type { AssimilateEvent } from "@/composables/useAssimilateUnit"
import { isSkippedFromAssimilationSequence } from "@/composables/useAssimilationSequenceSkip"
import { isSkippedForRecall } from "@/composables/useReviveMemoryTracker"
import { relationTypeLabelFromNoteContent } from "@/models/relationTypeOptions"
import { useInjectedMemoryTrackerActions } from "@/composables/useMemoryTrackerActions"
import { computed, ref, toRef } from "vue"
import {
  hasNoteLevelTrackerOfType,
  showRemoveFromRecall,
} from "./assimilationMemoryTrackers"

const { note, noteInfoLoaded, assimilateDisabled } = defineProps<{
  note: Note
  noteInfoLoaded: boolean
  assimilateDisabled: boolean
}>()

const emit = defineEmits<{
  (e: "assimilate", request: AssimilateEvent): void
  (e: "skip", request: { propertyKey?: string }): void
  (e: "revive", request: { propertyKey?: string }): void
  (e: "returnToSequence", request: { propertyKey?: string }): void
  (e: "removeFromRecall", request: { propertyKey?: string }): void
  (e: "refinementContentUpdated"): void
}>()

const showRefineNoteModal = ref(false)
const { noteRecallInfo } = useInjectedMemoryTrackerActions(toRef(() => note.id))

const hasNoteContent = computed(() => !!(note.content ?? "").trim())
const isLinkNote = computed(
  () => relationTypeLabelFromNoteContent(note.content) !== undefined
)

const showCommissionedOption = computed(
  () =>
    !hasNoteLevelTrackerOfType(
      noteRecallInfo.value?.memoryTrackers,
      "COMMISSIONED"
    )
)

const showSpellingOption = computed(
  () =>
    hasNoteContent.value &&
    !isLinkNote.value &&
    !hasNoteLevelTrackerOfType(noteRecallInfo.value?.memoryTrackers, "SPELLING")
)
</script>
