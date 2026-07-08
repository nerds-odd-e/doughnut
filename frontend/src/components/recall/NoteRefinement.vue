<template>
  <div
    v-if="refinementLayoutItems.length > 0"
    class="mb-4 rounded-lg bg-accent p-4"
    data-test-id="refinement-layout"
  >
    <div v-if="!showExtractionPreview" class="text-base">
      <div class="font-semibold mb-3 text-accent-content">
        Note layout:
      </div>
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
          @click="extractNote"
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

    <div
      v-else
      class="text-base"
      data-test-id="extraction-preview"
    >
      <div class="font-semibold mb-3 text-accent-content">
        Extract preview:
      </div>

      <div
        v-if="createError"
        class="daisy-alert daisy-alert-error mb-3 text-sm"
        data-test-id="extraction-preview-error"
      >
        {{ createError }}
      </div>

      <label class="block mb-3 text-accent-content">
        <span class="font-medium">Updated original note content</span>
        <textarea
          v-model="extractionPreview.updatedOriginalNoteContent"
          data-test-id="extraction-preview-original-content"
          class="daisy-textarea daisy-textarea-bordered mt-1 w-full min-h-24"
        />
      </label>

      <label class="block mb-3 text-accent-content">
        <span class="font-medium">New note title</span>
        <textarea
          v-model="extractionPreview.newNoteTitle"
          data-test-id="extraction-preview-new-title"
          class="daisy-textarea daisy-textarea-bordered mt-1 w-full"
          rows="1"
        />
      </label>

      <label class="block mb-3 text-accent-content">
        <span class="font-medium">New note content</span>
        <textarea
          v-model="extractionPreview.newNoteContent"
          data-test-id="extraction-preview-new-content"
          class="daisy-textarea daisy-textarea-bordered mt-1 w-full min-h-24"
        />
      </label>

      <div class="flex gap-2 mt-4">
        <button
          data-test-id="extraction-preview-back"
          class="daisy-btn daisy-btn-ghost daisy-btn-sm"
          @click="backToLayout"
        >
          Back
        </button>
        <button
          data-test-id="retry-extraction-preview"
          class="daisy-btn daisy-btn-ghost daisy-btn-sm"
          @click="retryExtractionPreview"
        >
          Ask AI to retry
        </button>
        <button
          data-test-id="extraction-preview-create"
          class="daisy-btn daisy-btn-primary daisy-btn-sm"
          :disabled="!canCreateExtractedNote"
          @click="createExtractedNote"
        >
          Create note
        </button>
      </div>
    </div>
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
import type {
  Note,
  NoteExtractionResult,
  NoteRefinementLayoutItem,
} from "@generated/doughnut-backend-api"
import { AiController } from "@generated/doughnut-backend-api/sdk.gen"

import {
  apiCallWithLoading,
  runWithBlockingApiLoading,
} from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"
import { useRefinementLayoutSelection } from "@/composables/useRefinementLayoutSelection"
import AiRequestExportDialog from "@/components/commons/AiRequestExportDialog.vue"
import usePopups from "../commons/Popups/usePopups"
import RefinementLayoutItemRow from "./RefinementLayoutItemRow.vue"
import { Folders } from "@lucide/vue"
import { computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const props = defineProps<{
  note: Note
}>()

const emit = defineEmits<{
  (e: "contentUpdated", newContent: string): void
}>()

const refinementLayoutItems = ref<NoteRefinementLayoutItem[]>([])
const showExtractionPreview = ref(false)
const extractionPreview = ref<NoteExtractionResult>({
  newNoteTitle: "",
  newNoteContent: "",
  updatedOriginalNoteContent: "",
})
const lastAiExtractionResult = ref<NoteExtractionResult | null>(null)
const createError = ref("")
const showExportExtractDialog = ref(false)
const showExportBreakdownDialog = ref(false)

const canCreateExtractedNote = computed(
  () => extractionPreview.value.newNoteTitle.trim().length > 0
)

const {
  selectedItemIds,
  isFullySelected,
  isPartiallySelected,
  setItemSelection,
  clearSelection,
} = useRefinementLayoutSelection(refinementLayoutItems)

const resetExtractionPreview = () => {
  showExtractionPreview.value = false
  createError.value = ""
  lastAiExtractionResult.value = null
  extractionPreview.value = {
    newNoteTitle: "",
    newNoteContent: "",
    updatedOriginalNoteContent: "",
  }
}

const loadRefinementLayout = async () => {
  try {
    const result = await apiCallWithLoading(() =>
      AiController.generateRefinementSuggestions({
        path: { note: props.note.id },
      })
    )

    refinementLayoutItems.value =
      !result.error && result.data?.items ? result.data.items : []
    clearSelection()
    resetExtractionPreview()
  } catch (err) {
    console.error("Failed to generate note layout:", err)
    refinementLayoutItems.value = []
    clearSelection()
    resetExtractionPreview()
  }
}

onMounted(() => loadRefinementLayout())

const { popups } = usePopups()
const router = useRouter()
const storageAccessor = useStorageAccessor()

const removeSelectedLayoutItems = async () => {
  if (selectedItemIds.value.length === 0) {
    return
  }

  const confirmed = await popups.confirm(
    `Are you sure you want to remove ${selectedItemIds.value.length} selected layout point(s)? The AI will remove related content from the note.`
  )

  if (!confirmed) {
    return
  }

  await runWithBlockingApiLoading(async () => {
    const { data, error } = await apiCallWithLoading(() =>
      AiController.removeRefinementSuggestion({
        path: { note: props.note.id },
        body: {
          layout: { items: refinementLayoutItems.value },
          selectedItemIds: selectedItemIds.value,
        },
      })
    )

    if (!error && data?.content !== undefined) {
      if (data.content === props.note.content) {
        return
      }

      const storedApi = storageAccessor.value?.storedApi()
      if (storedApi) {
        await storedApi.updateTextField(
          props.note.id,
          "edit content",
          data.content
        )
      }
      emit("contentUpdated", data.content)
      await loadRefinementLayout()
    }
  }, "AI is removing content...")
}

const fetchExtractionPreview = async () => {
  const response = await apiCallWithLoading(() =>
    AiController.extractNotePreview({
      path: { note: props.note.id },
      body: {
        layout: { items: refinementLayoutItems.value },
        selectedItemIds: selectedItemIds.value,
      },
    })
  )

  if (response.error || !response.data) {
    await popups.alert("Failed to generate extract preview")
    return false
  }

  extractionPreview.value = { ...response.data }
  lastAiExtractionResult.value = { ...response.data }
  createError.value = ""
  return true
}

const isExtractionPreviewEdited = () => {
  const lastResult = lastAiExtractionResult.value
  if (!lastResult) {
    return false
  }

  const current = extractionPreview.value
  return (
    current.newNoteTitle !== lastResult.newNoteTitle ||
    current.newNoteContent !== lastResult.newNoteContent ||
    current.updatedOriginalNoteContent !== lastResult.updatedOriginalNoteContent
  )
}

const runExtractionPreview = async (showPreviewOnSuccess: boolean) => {
  try {
    await runWithBlockingApiLoading(async () => {
      const success = await fetchExtractionPreview()
      if (success && showPreviewOnSuccess) {
        showExtractionPreview.value = true
      }
    }, "AI is generating preview...")
  } catch (err) {
    console.error("Failed to generate extract preview:", err)
    await popups.alert(`Error: ${err}`)
  }
}

const extractNote = () => runExtractionPreview(true)

const retryExtractionPreview = async () => {
  if (isExtractionPreviewEdited()) {
    const confirmed = await popups.confirm(
      "You have unsaved edits to the extract preview. Ask AI to retry will discard your edits and regenerate the preview."
    )
    if (!confirmed) {
      return
    }
  }

  await runExtractionPreview(false)
}

const backToLayout = () => {
  showExtractionPreview.value = false
  createError.value = ""
}

async function fetchExtractRequestExport() {
  const { data: response, error } = await AiController.exportExtractRequest({
    path: { note: props.note.id },
    body: {
      layout: { items: refinementLayoutItems.value },
      selectedItemIds: selectedItemIds.value,
    },
  })
  if (!error && response) {
    return response
  }
  return null
}

async function fetchBreakdownRequestExport() {
  const { data: response, error } =
    await AiController.exportRefinementLayoutRequest({
      path: { note: props.note.id },
    })
  if (!error && response) {
    return response
  }
  return null
}

const createExtractedNote = async () => {
  createError.value = ""

  try {
    await runWithBlockingApiLoading(async () => {
      const response = await apiCallWithLoading(() =>
        AiController.createExtractedNote({
          path: { note: props.note.id },
          body: {
            newNoteTitle: extractionPreview.value.newNoteTitle,
            newNoteContent: extractionPreview.value.newNoteContent,
            updatedOriginalNoteContent:
              extractionPreview.value.updatedOriginalNoteContent,
          },
        })
      )

      if (response.error || !response.data) {
        const openApiError = toOpenApiError(response.error)
        createError.value =
          openApiError.message ?? "Failed to create note from preview"
        return
      }

      await storageAccessor.value
        .storedApi()
        .focusNoteRealm(router, response.data)
    }, "AI is creating note...")
  } catch (err) {
    console.error("Failed to create extracted note:", err)
    createError.value = `Error: ${err}`
  }
}
</script>
