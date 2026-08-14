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
    <NoteInfoBar
      ref="noteInfoBarRef"
      :note-id="note.id"
      :note="note"
      @level-changed="emit('levelChanged', $event)"
      @note-recall-info-loaded="onNoteRecallInfoLoaded"
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
                    assimilateDisabledForProperty(row.key)
                  "
                  :skipped-for-recall="
                    isSkippedForRecall(noteRecallInfo, row.key)
                  "
                  @assimilate="
                    emit('assimilate', {
                      skipMemoryTracking: false,
                      propertyKey: row.key,
                    })
                  "
                  @skip="emit('skip', { propertyKey: row.key })"
                  @revive="emit('revive', { propertyKey: row.key })"
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
          :show-commissioned-option="showCommissionedOption"
          :show-spelling-option="showSpellingOption"
          @assimilate="emit('assimilate', { skipMemoryTracking: false })"
          @skip="emit('skip', {})"
          @assimilate-as-commissioned="
            emit('assimilate', {
              skipMemoryTracking: false,
              assimilateAsCommissioned: true,
            })
          "
          @remember-spelling="
            emit('assimilate', {
              skipMemoryTracking: false,
              assimilateAsSpelling: true,
            })
          "
          @revive="emit('revive', {})"
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
import type { Note, NoteRecallInfo } from "@generated/doughnut-backend-api"
import NoteInfoBar from "../notes/NoteInfoBar.vue"
import AssimilationButtons from "./AssimilationButtons.vue"
import AssimilationProgressSummary from "./AssimilationProgressSummary.vue"
import RefineNoteModal from "./RefineNoteModal.vue"
import type { AssimilateEvent } from "@/composables/useAssimilateUnit"
import { isSkippedForRecall } from "@/composables/useReviveMemoryTracker"
import { relationTypeLabelFromNoteContent } from "@/models/relationTypeOptions"
import {
  parseNoteContentMarkdown,
  sortedPropertyRowsFromNoteProperties,
} from "@/utils/noteContentFrontmatter"
import { compactDisplayForPropertyValue } from "@/utils/noteProperties"
import { usePendingAssimilationProperty } from "@/composables/usePendingAssimilationProperty"
import { computed, ref, toRef } from "vue"
import { hasNoteLevelTrackerOfType } from "./noteLevelMemoryTrackers"

const { note, noteInfoLoaded, assimilateDisabled, assimilatingPropertyKey } =
  defineProps<{
    note: Note
    noteInfoLoaded: boolean
    assimilateDisabled: boolean
    assimilatingPropertyKey?: string | null
  }>()

const emit = defineEmits<{
  (e: "levelChanged", value: unknown): void
  (e: "noteRecallInfoLoaded", value: NoteRecallInfo): void
  (e: "assimilate", request: AssimilateEvent): void
  (e: "skip", request: { propertyKey?: string }): void
  (e: "revive", request: { propertyKey?: string }): void
  (e: "refinementContentUpdated"): void
}>()

const showRefineNoteModal = ref(false)
const noteInfoBarRef = ref<InstanceType<typeof NoteInfoBar> | null>(null)
const noteRecallInfo = ref<NoteRecallInfo | null>(null)
const { propertiesSectionOpen, isPendingProperty, setPropertyRowRef } =
  usePendingAssimilationProperty(toRef(() => note.id))

const propertyRows = computed(() => {
  const parsed = parseNoteContentMarkdown(note.content ?? "")
  if (!parsed.ok) return []
  return sortedPropertyRowsFromNoteProperties(parsed.properties)
})

const onNoteRecallInfoLoaded = (info: NoteRecallInfo) => {
  noteRecallInfo.value = info
  emit("noteRecallInfoLoaded", info)
}

const assimilateDisabledForProperty = (propertyKey: string) =>
  noteRecallInfo.value?.memoryTrackers?.some(
    (mt) => mt.propertyKey === propertyKey
  ) ?? false

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

const reloadNoteInfo = async () => {
  await noteInfoBarRef.value?.reload()
  noteRecallInfo.value =
    noteInfoBarRef.value?.noteRecallInfo ?? noteRecallInfo.value
}

defineExpose({ reloadNoteInfo })
</script>
