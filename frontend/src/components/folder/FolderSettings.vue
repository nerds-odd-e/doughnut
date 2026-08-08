<template>
  <div
    class="folder-settings"
    data-testid="folder-settings"
  >
    <div class="daisy-card w-full mb-6" data-testid="folder-move-dialog">
      <div class="daisy-card-body">
        <form @submit.prevent="() => submitMove()">
          <fieldset :disabled="processing">
            <p class="text-sm mb-3">
              Move folder "{{ folderRealm.folder.name }}".
            </p>
            <label class="daisy-label" for="folder-move-notebook">
              <span class="daisy-label-text">Destination notebook</span>
            </label>
            <select
              id="folder-move-notebook"
              v-model="selectedDestinationNotebookId"
              class="daisy-select w-full mb-3"
              data-testid="folder-move-notebook-select"
              :disabled="processing || notebooksLoading"
            >
              <option
                v-for="notebook in destinationNotebooks"
                :key="notebook.id"
                :value="notebook.id"
              >
                {{ notebook.name }}
              </option>
            </select>
            <p v-if="notebooksLoadError" class="text-error text-sm mb-2">
              {{ notebooksLoadError }}
            </p>
            <label class="daisy-label" for="folder-move-destination">
              <span class="daisy-label-text">Destination folder</span>
            </label>
            <div id="folder-move-destination">
              <FolderSelector
                :key="folderPickerNotebookId"
                v-model="selectedParentFolder"
                :notebook-id="folderPickerNotebookId"
                :context-folder="folderPickerContextFolder"
                :ancestor-folders="folderPickerAncestorFolders"
                :disabled="processing"
              />
            </div>
            <p v-if="moveError" class="text-error text-sm mt-2">
              {{ moveError }}
            </p>
            <button
              type="submit"
              class="daisy-btn daisy-btn-primary mt-4"
              data-testid="folder-move-submit"
              :disabled="processing"
            >
              Move folder
            </button>
          </fieldset>
        </form>
        <div class="daisy-divider my-4">or</div>
        <p class="text-sm mb-2">
          Dissolve "{{ folderRealm.folder.name }}". Notes and subfolders will move
          to {{ dissolveParentLabel }}.
        </p>
        <p v-if="dissolveError" class="text-error text-sm mt-2">
          {{ dissolveError }}
        </p>
        <button
          type="button"
          class="daisy-btn daisy-btn-error daisy-btn-outline"
          data-testid="folder-dissolve-button"
          :disabled="processing"
          @click="() => dissolve()"
        >
          Dissolve folder
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { FolderRealm } from "@generated/doughnut-backend-api"
import { toRef } from "vue"
import FolderSelector from "@/components/notes/FolderSelector.vue"
import { useFolderAdmin } from "@/composables/useFolderAdmin"

const props = defineProps<{
  folderRealm: FolderRealm
  fetchFolderPage: () => Promise<void>
}>()

const {
  processing,
  moveError,
  dissolveError,
  selectedParentFolder,
  destinationNotebooks,
  notebooksLoading,
  notebooksLoadError,
  selectedDestinationNotebookId,
  folderPickerNotebookId,
  folderPickerContextFolder,
  folderPickerAncestorFolders,
  dissolveParentLabel,
  submitMove,
  dissolve,
} = useFolderAdmin(toRef(props, "folderRealm"), props.fetchFolderPage)
</script>
