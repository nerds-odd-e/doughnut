<template>
  <footer
    class="relative w-full mt-4 pointer-events-none z-40"
    aria-label="Assimilation settings"
  >
    <div
      class="assimilation-settings-inner pointer-events-auto w-full px-4 pb-[max(0.75rem,env(safe-area-inset-bottom))] pt-0"
    >
      <div
        class="daisy-card bg-base-100 border border-base-300 shadow-xl"
      >
        <div class="daisy-card-body gap-0 p-4">
          <h2 class="daisy-card-title mb-3 text-base font-semibold gap-2">
            Assimilation settings
            <AssimilationProgressSummary />
          </h2>
          <div
            class="assimilation-settings-scroll max-h-[min(40vh,22rem)] overflow-y-auto pr-1 pb-20"
          >
            <NoteInfoBar
              ref="noteInfoBarRef"
              :note-id="note.id"
              :note="note"
              @level-changed="emit('levelChanged', $event)"
              @remember-spelling-changed="emit('rememberSpellingChanged', $event)"
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
                            (skip) =>
                              emit('assimilate', {
                                skipMemoryTracking: skip,
                                propertyKey: row.key,
                              })
                          "
                          @revive="emit('revive', { propertyKey: row.key })"
                        />
                      </span>
                    </li>
                  </ul>
                </div>
              </div>
            </section>
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
                :assimilate-disabled="assimilateDisabled"
                :skipped-for-recall="isSkippedForRecall(noteRecallInfo)"
                :show-commissioned-option="showCommissionedOption"
                @assimilate="
                  (skip) => emit('assimilate', { skipMemoryTracking: skip })
                "
                @assimilate-as-commissioned="
                  emit('assimilate', {
                    skipMemoryTracking: false,
                    assimilateAsCommissioned: true,
                  })
                "
                @revive="emit('revive', {})"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </footer>
  <RefineNoteModal
    v-if="(note.content ?? '').trim()"
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
import {
  parseNoteContentMarkdown,
  sortedPropertyRowsFromNoteProperties,
} from "@/utils/noteContentFrontmatter"
import { compactDisplayForPropertyValue } from "@/utils/noteProperties"
import { usePendingAssimilationProperty } from "@/composables/usePendingAssimilationProperty"
import { computed, ref, toRef } from "vue"

const { note, noteInfoLoaded, assimilateDisabled, assimilatingPropertyKey } =
  defineProps<{
    note: Note
    noteInfoLoaded: boolean
    assimilateDisabled: boolean
    assimilatingPropertyKey?: string | null
  }>()

const emit = defineEmits<{
  (e: "levelChanged", value: unknown): void
  (e: "rememberSpellingChanged", value: boolean): void
  (e: "noteRecallInfoLoaded", value: NoteRecallInfo): void
  (e: "assimilate", request: AssimilateEvent): void
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

const showCommissionedOption = computed(
  () =>
    !(
      noteRecallInfo.value?.memoryTrackers?.some(
        (mt) => !mt.propertyKey && mt.type === "COMMISSIONED"
      ) ?? false
    )
)

const reloadNoteInfo = async () => {
  await noteInfoBarRef.value?.reload()
  noteRecallInfo.value =
    noteInfoBarRef.value?.noteRecallInfo ?? noteRecallInfo.value
}

defineExpose({ reloadNoteInfo })
</script>
