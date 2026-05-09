<template>
  <div class="daisy-card daisy-w-full" data-testid="folder-new-dialog">
    <div class="daisy-card-body">
      <form @submit.prevent="processForm">
        <fieldset :disabled="processing">
          <label class="daisy-label daisy-mb-2">
            <span class="daisy-label-text">Parent folder</span>
          </label>
          <div class="daisy-mb-4">
            <FolderSelector
              v-model="selectedParentFolderId"
              :notebook-id="notebookId"
              :context-folder-id="contextFolderId"
              :ancestor-folders="ancestorFolders"
              :disabled="processing"
            />
          </div>
          <PathNameEditor
            v-model="name"
            :error-message="nameError"
            autofocus
            label-text="Folder name"
            editor-role="textbox"
            placeholder="Folder name"
            editor-data-test="folder-name"
          />
          <input
            type="submit"
            value="Submit"
            class="daisy-btn daisy-btn-primary daisy-mt-4"
            data-testid="folder-new-dialog-submit"
          />
        </fieldset>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Folder } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { ref, watch } from "vue"
import PathNameEditor from "@/components/notes/core/PathNameEditor.vue"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import FolderSelector from "./FolderSelector.vue"

const props = defineProps<{
  notebookId: number
  ancestorFolders: Folder[]
  contextFolderId: number | null
  initialParentFolderId: number | null
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const name = ref("")
const nameError = ref<string | undefined>(undefined)
const processing = ref(false)

const selectedParentFolderId = ref<number | null>(null)

watch(
  () => props.initialParentFolderId,
  (v) => {
    selectedParentFolderId.value = v
  },
  { immediate: true }
)

const processForm = async () => {
  if (processing.value) return
  processing.value = true
  nameError.value = undefined
  try {
    const { data, error } = await apiCallWithLoading(() =>
      NotebookController.createFolder({
        path: { notebook: props.notebookId },
        body: {
          name: name.value,
          ...(selectedParentFolderId.value != null
            ? { underFolderId: selectedParentFolderId.value }
            : {}),
        },
      })
    )
    if (error || !data) throw error ?? new Error("Failed to create folder")
    refreshSidebarStructuralListings()
    emit("closeDialog")
  } catch (res: unknown) {
    nameError.value = toOpenApiError(res).message ?? "Failed to create folder"
  } finally {
    processing.value = false
  }
}
</script>
