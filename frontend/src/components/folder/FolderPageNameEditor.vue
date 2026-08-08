<template>
  <PathNameEditor
    :model-value="localValue"
    :error-message="nameError"
    hide-label
    editor-role="heading"
    editor-data-test="folder-page-name"
    @update:model-value="proposeFolderName"
    @blur="flushFolderName"
  >
    <template #title="{ bindings, editor }">
      <h1 class="text-xl font-semibold text-base-content">
        <component :is="editor" v-bind="bindings" />
      </h1>
    </template>
  </PathNameEditor>
</template>

<script setup lang="ts">
import type { FolderRealm } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { ref } from "vue"
import PathNameEditor from "@/components/notes/core/PathNameEditor.vue"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import { useDebouncedTextAutosave } from "@/composables/useDebouncedTextAutosave"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"

const props = defineProps<{
  folderRealm: FolderRealm
  fetchFolderPage: () => Promise<void>
}>()

const nameError = ref<string | undefined>(undefined)

const persistFolderName = async (name: string) => {
  const { error } = await apiCallWithLoading(() =>
    NotebookController.renameFolder({
      path: {
        notebook: props.folderRealm.notebookRealm.notebook.id,
        folder: props.folderRealm.folder.id,
      },
      body: { name },
    })
  )
  if (error) throw error
  refreshSidebarStructuralListings()
  await props.fetchFolderPage()
}

const {
  localValue,
  propose,
  flush: flushAutosave,
  cancel,
} = useDebouncedTextAutosave({
  externalValue: () => props.folderRealm.folder.name,
  persist: persistFolderName,
  normalize: (value) => value.trim(),
  onError: (error) => {
    const apiError = toOpenApiError(error)
    nameError.value =
      apiError.errors?.name ?? apiError.message ?? "Failed to rename folder"
  },
})

const proposeFolderName = (value: string) => {
  cancel()
  nameError.value = undefined
  propose(value)
  if (value.trim() === "") {
    cancel()
    nameError.value =
      "Folder name cannot be empty. Enter a name to rename this folder."
  }
}

const flushFolderName = () => {
  if (localValue.value.trim() === "") {
    cancel()
    return
  }
  flushAutosave()
}
</script>
