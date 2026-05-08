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
            <FolderSelector
              v-model="selectedParentFolderId"
              :notebook-id="notebookId"
              :context-folder-id="movingFolderId"
              :ancestor-folders="ancestorFolders"
              :disabled="processing"
            />
          </div>
          <p v-if="moveError" class="daisy-text-error daisy-text-sm daisy-mt-2">
            {{ moveError }}
          </p>
          <button
            type="submit"
            class="daisy-btn daisy-btn-primary daisy-mt-4"
            data-testid="folder-move-submit"
            :disabled="processing"
          >
            Move folder
          </button>
        </fieldset>
      </form>
      <div class="daisy-divider daisy-my-4">or</div>
      <p class="daisy-text-sm daisy-mb-2">
        Dissolve "{{ movingFolderName }}". Notes and subfolders will move to
        {{ dissolveParentLabel }}.
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
import type { Folder } from "@generated/doughnut-backend-api"
import { computed, ref } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { toOpenApiError } from "@/managedApi/openApiError"
import usePopups from "../commons/Popups/usePopups"
import { notebookSidebarUserActiveFolder } from "@/composables/useCurrentNoteSidebarState"
import FolderSelector from "./FolderSelector.vue"
import { dissolveParentLabelFromChain } from "./folderSelectorUtils"

const props = defineProps<{
  notebookId: number
  movingFolderId: number
  movingFolderName: string
  /** Root-to-leaf ancestor chain from NoteRealm (may include the moving folder). */
  ancestorFolders: Folder[]
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const storageAccessor = useStorageAccessor()
const { popups } = usePopups()

const processing = ref(false)
const moveError = ref<string | undefined>(undefined)
const dissolveError = ref<string | undefined>(undefined)
const selectedParentFolderId = ref<number | null>(null)

const dissolveParentLabel = computed(() =>
  dissolveParentLabelFromChain(props.movingFolderId, props.ancestorFolders)
)

const submitMove = async () => {
  if (processing.value) return
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
