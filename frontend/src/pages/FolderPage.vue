<template>
  <ContentLoader v-if="folderForView === undefined" />
  <div v-else class="daisy-py-4">
    <NotebookPageReadonlySummary
      v-if="folderForView.notebookRealm.readonly === true"
      :notebook="folderForView.notebookRealm.notebook"
    />
    <div v-else class="daisy-container daisy-mx-auto daisy-max-w-6xl">
      <p class="daisy-text-sm daisy-text-base-content/70 daisy-mb-4">
        Folder
        <span class="daisy-font-semibold daisy-text-base-content">{{
          folderForView.folder.name
        }}</span>
      </p>
      <ScopedIndexNoteEditor
        :notebook-id="folderForView.notebookRealm.notebook.id"
        :folder-id="folderForView.folder.id"
        :index-content="folderForView.indexContent ?? null"
        test-id-prefix="folder-index"
        rich-editor-scope-name="folder-index"
        heading-label="Folder index"
        save-button-idle-label="Save folder index"
        save-button-saving-label="Saving…"
        success-toast-saved="Folder index saved"
        @saved="fetchFolderPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import type { FolderRealm } from "@generated/doughnut-backend-api"
import NotebookPageReadonlySummary from "@/components/notebook/NotebookPageReadonlySummary.vue"
import ScopedIndexNoteEditor from "@/components/notebook/ScopedIndexNoteEditor.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"

const props = defineProps<{
  folderRealm: FolderRealm | undefined
  fetchFolderPage: () => Promise<void>
}>()

const folderForView = computed((): FolderRealm | undefined => {
  const r = props.folderRealm
  if (r?.notebookRealm?.notebook == null) return undefined
  return r
})
</script>
