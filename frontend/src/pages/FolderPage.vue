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
        :index-note-status="indexNoteStatus"
        :index-note-id="sidebarAnchorNoteId"
        :fetch-page="fetchFolderPage"
        test-id-prefix="folder-index"
        rich-editor-scope-name="folder-index"
        loading-hint="Loading folder index…"
        absent-heading="Folder index"
        absent-help-before="No index note yet. Edit below and save to create a note titled "
        absent-help-after=" in this folder."
        present-title-fallback="Folder index"
        save-button-idle-label="Save folder index"
        save-button-saving-label="Saving…"
        success-toast-saved="Folder index saved"
        success-toast-after-race="Folder index is now available"
        error-toast-after-race="Could not create folder index: a conflicting note may exist. Refresh the page and try again."
        @index-note-created="fetchFolderPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue"
import type { FolderRealm } from "@generated/doughnut-backend-api"
import NotebookPageReadonlySummary from "@/components/notebook/NotebookPageReadonlySummary.vue"
import ScopedIndexNoteEditor from "@/components/notebook/ScopedIndexNoteEditor.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const props = defineProps<{
  folderRealm: FolderRealm | undefined
  fetchFolderPage: () => Promise<void>
}>()

const storageAccessor = useStorageAccessor()

const folderForView = computed((): FolderRealm | undefined => {
  const r = props.folderRealm
  if (r?.notebookRealm?.notebook == null) return undefined
  return r
})

const sidebarAnchorNoteId = ref<number | undefined>()
const indexNoteStatus = ref<"pending" | "present" | "absent">("pending")
let indexResolveGeneration = 0

watch(
  () =>
    props.folderRealm?.folder
      ? ([
          props.folderRealm.folder.id,
          props.folderRealm.folderIndexNoteId ?? null,
        ] as const)
      : undefined,
  async (key) => {
    if (key === undefined) {
      sidebarAnchorNoteId.value = undefined
      indexNoteStatus.value = "pending"
      return
    }

    const [, folderIndexNoteId] = key
    const gen = ++indexResolveGeneration
    sidebarAnchorNoteId.value = undefined
    indexNoteStatus.value = "pending"

    if (folderIndexNoteId == null) {
      indexNoteStatus.value = "absent"
      return
    }

    try {
      await storageAccessor.value.storedApi().loadNoteRealm(folderIndexNoteId)
      if (gen !== indexResolveGeneration) return
      sidebarAnchorNoteId.value = folderIndexNoteId
      indexNoteStatus.value = "present"
    } catch {
      if (gen !== indexResolveGeneration) return
      indexNoteStatus.value = "absent"
    }
  },
  { immediate: true }
)
</script>
