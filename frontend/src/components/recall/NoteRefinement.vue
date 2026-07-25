<template>
  <div
    v-if="refinementLayoutItems.length > 0"
    class="mb-4 rounded-lg bg-accent p-4"
    data-test-id="refinement-layout"
  >
    <div v-if="!showExtractionPreview" class="text-base">
      <div class="font-semibold mb-3 text-accent-content">Note layout:</div>
      <ul class="space-y-2">
        <li
          v-for="item in refinementLayoutItems"
          :key="item.id"
          :data-test-id="`refinement-layout-item-${item.id}`"
          data-layout-level="1"
          class="text-accent-content"
        >
          <RefinementLayoutItemRow
            :item="item"
            :fully-selected="isFullySelected(item)"
            :partially-selected="isPartiallySelected(item)"
            @selection-change="setItemSelection(item, $event)"
          />

          <ul v-if="item.children.length > 0" class="ml-6 mt-2 space-y-2">
            <li
              v-for="child in item.children"
              :key="child.id"
              :data-test-id="`refinement-layout-item-${child.id}`"
              data-layout-level="2"
            >
              <RefinementLayoutItemRow
                :item="child"
                :fully-selected="isFullySelected(child)"
                :partially-selected="isPartiallySelected(child)"
                @selection-change="setItemSelection(child, $event)"
              />
            </li>
          </ul>
        </li>
      </ul>

      <div class="flex gap-2 mt-4">
        <button
          data-test-id="extract-refinement-layout"
          :disabled="selectedItemIds.length === 0"
          @click="openExtractionPreview"
          class="daisy-btn daisy-btn-primary daisy-btn-sm"
          title="Extract selected to a new note"
        >
          <Folders class="w-4 h-4" />
          Extract
        </button>
        <button
          data-test-id="export-extract-request"
          :disabled="selectedItemIds.length === 0"
          @click="showExportExtractDialog = true"
          class="daisy-btn daisy-btn-ghost daisy-btn-sm"
          title="Export extract request for ChatGPT"
        >
          Export extract request
        </button>
        <button
          data-test-id="export-breakdown-request"
          @click="showExportBreakdownDialog = true"
          class="daisy-btn daisy-btn-ghost daisy-btn-sm"
          title="Export breakdown request for ChatGPT"
        >
          Export breakdown request
        </button>
        <button
          data-test-id="remove-refinement-layout"
          :disabled="selectedItemIds.length === 0"
          @click="removeSelectedLayoutItems"
          class="daisy-btn daisy-btn-error daisy-btn-sm !text-white"
        >
          Remove selected
        </button>
      </div>
    </div>

    <NoteExtractionPreview
      v-else
      v-model="extractionPreview"
      :original-note-content="note.content ?? ''"
      :create-error="createError"
      :can-create="canCreateExtractedNote"
      @back="backToLayout"
      @retry="retryExtractionPreview"
      @create="createExtractedNote"
    />
  </div>

  <div
    v-else-if="layoutLoadSettled"
    class="mb-4 rounded-lg bg-accent p-4"
    data-test-id="refinement-layout-empty"
  >
    <div class="font-semibold mb-3 text-accent-content">Note layout:</div>
    <button
      data-test-id="retry-refinement-layout"
      class="daisy-btn daisy-btn-ghost daisy-btn-sm"
      @click="() => loadRefinementLayout()"
    >
      Ask AI to retry
    </button>
  </div>

  <AiRequestExportDialog
    v-if="showExportExtractDialog"
    title="Export Extract Request for ChatGPT"
    :fetch-export="fetchExtractRequestExport"
    @close="showExportExtractDialog = false"
  />

  <AiRequestExportDialog
    v-if="showExportBreakdownDialog"
    title="Export Breakdown Request for ChatGPT"
    :fetch-export="fetchBreakdownRequestExport"
    @close="showExportBreakdownDialog = false"
  />
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import AiRequestExportDialog from "@/components/commons/AiRequestExportDialog.vue"
import { useNoteRefinement } from "@/composables/useNoteRefinement"
import NoteExtractionPreview from "./NoteExtractionPreview.vue"
import RefinementLayoutItemRow from "./RefinementLayoutItemRow.vue"
import { Folders } from "@lucide/vue"
import { toRef } from "vue"

const props = defineProps<{
  note: Note
}>()

const emit = defineEmits<{
  (e: "contentUpdated", newContent: string): void
}>()

const {
  refinementLayoutItems,
  layoutLoadSettled,
  showExtractionPreview,
  extractionPreview,
  createError,
  showExportExtractDialog,
  showExportBreakdownDialog,
  canCreateExtractedNote,
  selectedItemIds,
  isFullySelected,
  isPartiallySelected,
  setItemSelection,
  loadRefinementLayout,
  removeSelectedLayoutItems,
  openExtractionPreview,
  retryExtractionPreview,
  backToLayout,
  fetchExtractRequestExport,
  fetchBreakdownRequestExport,
  createExtractedNote,
} = useNoteRefinement(toRef(props, "note"), (newContent) =>
  emit("contentUpdated", newContent)
)
</script>
