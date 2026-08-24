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
      :note="note"
      :note-recall-info="noteRecallInfo"
      @level-changed="emit('levelChanged', $event)"
    />
    <section
      v-if="propertyRows.length > 0"
      data-test="assimilation-properties-section"
      class="mt-4"
    >
      <div
        class="daisy-collapse daisy-collapse-arrow border border-base-300 bg-base-200/50 rounded-lg"
      >
        <input
          v-model="propertiesSectionOpen"
          type="checkbox"
          data-test="assimilation-properties-toggle"
        />
        <div class="daisy-collapse-title min-h-0 py-3 text-sm font-medium">
          Properties
        </div>
        <div class="daisy-collapse-content">
          <ul class="flex flex-col gap-2 pb-3">
            <li
              v-for="row in propertyRows"
              :key="row.key"
              :ref="(el) => setPropertyRowRef(row.key, el)"
              data-test="assimilation-property-row"
              :data-property-key="row.key"
              :data-test-pending="
                isPendingProperty(row.key) ? 'true' : undefined
              "
              class="flex flex-wrap items-center gap-2 gap-y-1 border-t border-base-300 pt-2 first:border-t-0 first:pt-0"
              :class="{
                'rounded bg-primary/10 ring-1 ring-primary/30':
                  isPendingProperty(row.key),
              }"
            >
              <span class="font-medium shrink-0">{{ row.key }}</span>
              <span
                class="min-w-0 flex-1 truncate text-sm text-base-content/70"
                :title="compactDisplayForPropertyValue(row.value)"
              >{{ compactDisplayForPropertyValue(row.value) }}</span>
              <span class="shrink-0">
                <AssimilationButtons
                  size="sm"
                  :disabled="assimilatingPropertyKey === row.key"
                  :assimilate-disabled="
                    assimilateDisabledForProperty(noteRecallInfo, row.key)
                  "
                  :skipped-for-recall="
                    isSkippedForRecall(noteRecallInfo, row.key)
                  "
                  :skipped-from-assimilation-sequence="
                    isSkippedFromAssimilationSequence(noteRecallInfo, row.key)
                  "
                  :show-remove-from-recall="
                    showRemoveFromRecall(noteRecallInfo, row.key)
                  "
                  @assimilate="
                    emit('assimilate', {
                      propertyKey: row.key,
                    })
                  "
                  @skip="emit('skip', { propertyKey: row.key })"
                  @revive="emit('revive', { propertyKey: row.key })"
                  @return-to-sequence="
                    emit('returnToSequence', { propertyKey: row.key })
                  "
                  @remove-from-recall="
                    emit('removeFromRecall', { propertyKey: row.key })
                  "
                />
              </span>
            </li>
          </ul>
        </div>
      </div>
    </section>
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
import type { Note } from "@generated/doughnut-backend-api"
import NoteInfoComponent from "../notes/NoteInfoComponent.vue"
import AssimilationButtons from "./AssimilationButtons.vue"
import AssimilationProgressSummary from "./AssimilationProgressSummary.vue"
import RefineNoteModal from "./RefineNoteModal.vue"
import type { AssimilateEvent } from "@/composables/useAssimilateUnit"
import { isSkippedFromAssimilationSequence } from "@/composables/useAssimilationSequenceSkip"
import { isSkippedForRecall } from "@/composables/useReviveMemoryTracker"
import { relationTypeLabelFromNoteContent } from "@/models/relationTypeOptions"
import {
  parseNoteContentMarkdown,
  sortedPropertyRowsFromNoteProperties,
} from "@/utils/noteContentFrontmatter"
import { compactDisplayForPropertyValue } from "@/utils/noteProperties"
import { usePendingAssimilationProperty } from "@/composables/usePendingAssimilationProperty"
import { useInjectedMemoryTrackerActions } from "@/composables/useMemoryTrackerActions"
import { computed, ref, toRef } from "vue"
import {
  hasNoteLevelTrackerOfType,
  assimilateDisabledForProperty,
  showRemoveFromRecall,
} from "./assimilationMemoryTrackers"

const { note, noteInfoLoaded, assimilateDisabled, assimilatingPropertyKey } =
  defineProps<{
    note: Note
    noteInfoLoaded: boolean
    assimilateDisabled: boolean
    assimilatingPropertyKey?: string | null
  }>()

const emit = defineEmits<{
  (e: "levelChanged", value: unknown): void
  (e: "assimilate", request: AssimilateEvent): void
  (e: "skip", request: { propertyKey?: string }): void
  (e: "revive", request: { propertyKey?: string }): void
  (e: "returnToSequence", request: { propertyKey?: string }): void
  (e: "removeFromRecall", request: { propertyKey?: string }): void
  (e: "refinementContentUpdated"): void
}>()

const showRefineNoteModal = ref(false)
const { noteRecallInfo } = useInjectedMemoryTrackerActions(toRef(() => note.id))
const { propertiesSectionOpen, isPendingProperty, setPropertyRowRef } =
  usePendingAssimilationProperty(toRef(() => note.id))

const propertyRows = computed(() => {
  const parsed = parseNoteContentMarkdown(note.content ?? "")
  if (!parsed.ok) return []
  return sortedPropertyRowsFromNoteProperties(parsed.properties)
})

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
