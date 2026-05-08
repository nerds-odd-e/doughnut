<template>
  <div class="daisy-card daisy-w-full" data-testid="folder-move-dialog">
    <div class="daisy-card-body">
      <form @submit.prevent="submitMove">
        <fieldset>
          <p class="daisy-text-sm daisy-mb-3">
            Move folder "{{ movingFolderName }}".
          </p>
          <label class="daisy-label" for="folder-move-destination">
            <span class="daisy-label-text">Destination</span>
          </label>
          <div id="folder-move-destination">
            <p
              v-if="!optionsReady"
              class="daisy-text-sm daisy-text-base-content/70 daisy-mb-2"
            >
              Loading destinations…
            </p>
            <FolderSelector
              v-if="optionsReady"
              v-model="selectedParentFolderId"
              :notebook-id="notebookId"
              :context-folder-id="movingFolderId"
              :excluded-folder-ids="excludedSubtreeIds"
              :folder-index-rows="folderIndexRows"
              :disabled="processing"
            />
          </div>
          <p v-if="loadError" class="daisy-text-error daisy-text-sm daisy-mt-2">
            {{ loadError }}
          </p>
          <p v-if="moveError" class="daisy-text-error daisy-text-sm daisy-mt-2">
            {{ moveError }}
          </p>
          <button
            type="submit"
            class="daisy-btn daisy-btn-primary daisy-mt-4"
            data-testid="folder-move-submit"
            :disabled="processing || !optionsReady"
          >
            Move folder
          </button>
        </fieldset>
      </form>
      <div class="daisy-divider daisy-my-4">or</div>
      <p class="daisy-text-sm daisy-mb-2">
        Dissolve "{{ movingFolderName }}". Notes and subfolders will move to
        {{ parentLocationLabel }}.
      </p>
      <p
        v-if="dissolveError"
        class="daisy-text-error daisy-text-sm daisy-mt-2"
      >
        {{ dissolveError }}
      </p>
      <button
        type="button"
        class="daisy-btn daisy-btn-error daisy-btn-outline"
        data-testid="folder-dissolve-button"
        :disabled="processing"
        @click="dissolve"
      >
        Dissolve folder
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { NotebookFolderIndexRow } from "@generated/doughnut-backend-api"
import { onMounted, ref } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { toOpenApiError } from "@/managedApi/openApiError"
import usePopups from "../commons/Popups/usePopups"
import { notebookSidebarUserActiveFolder } from "@/composables/useCurrentNoteSidebarState"
import FolderSelector from "./FolderSelector.vue"
import {
  collectSubtreeFolderIds,
  dissolveParentQuotedLabel,
  folderRowsById,
} from "./folderSelectorUtils"

const props = defineProps<{
  notebookId: number
  movingFolderId: number
  movingFolderName: string
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const storageAccessor = useStorageAccessor()
const { popups } = usePopups()

const processing = ref(false)
const optionsReady = ref(false)
const loadError = ref<string | undefined>(undefined)
const moveError = ref<string | undefined>(undefined)
const dissolveError = ref<string | undefined>(undefined)
const selectedParentFolderId = ref<number | null>(null)
const folderIndexRows = ref<NotebookFolderIndexRow[]>([])
const excludedSubtreeIds = ref(new Set<number>())
const parentLocationLabel = ref("notebook root")

onMounted(async () => {
  loadError.value = undefined
  try {
    const rows = await storageAccessor.value
      .storedApi()
      .loadNotebookFolderIndex(props.notebookId)
    folderIndexRows.value = rows
    excludedSubtreeIds.value = collectSubtreeFolderIds(
      props.movingFolderId,
      rows
    )
    const byId = folderRowsById(rows)
    parentLocationLabel.value = dissolveParentQuotedLabel(
      props.movingFolderId,
      byId
    )
    optionsReady.value = true
  } catch (e: unknown) {
    loadError.value = toOpenApiError(e).message ?? "Failed to load folders"
  }
})

const submitMove = async () => {
  if (processing.value || !optionsReady.value) return
  processing.value = true
  moveError.value = undefined
  try {
    await storageAccessor.value
      .storedApi()
      .moveFolder(
        props.notebookId,
        props.movingFolderId,
        selectedParentFolderId.value
      )
    emit("closeDialog")
  } catch (e: unknown) {
    moveError.value = toOpenApiError(e).message ?? "Failed to move folder"
  } finally {
    processing.value = false
  }
}

const dissolve = async () => {
  if (processing.value) return
  const ok = await popups.confirm(
    `Dissolve folder "${props.movingFolderName}"? Notes and subfolders will be kept.`
  )
  if (!ok) return
  processing.value = true
  dissolveError.value = undefined
  try {
    await storageAccessor.value
      .storedApi()
      .dissolveFolder(props.notebookId, props.movingFolderId)
    notebookSidebarUserActiveFolder.value = null
    emit("closeDialog")
  } catch (e: unknown) {
    dissolveError.value =
      toOpenApiError(e).message ?? "Failed to dissolve folder"
  } finally {
    processing.value = false
  }
}
</script>
