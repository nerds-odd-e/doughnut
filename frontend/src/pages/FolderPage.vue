<template>
  <ContentLoader v-if="folderForView === undefined" />
  <div v-else class="pt-0 pb-4">
    <NotebookPageReadonlySummary
      v-if="folderForView.notebookRealm.readonly === true"
      :notebook="folderForView.notebookRealm.notebook"
    />
    <div v-else class="container mx-auto pt-0 pb-4 max-w-6xl">
      <div class="folder-page-summary mb-6" data-testid="folder-page-summary">
        <p
          class="text-sm text-base-content/70 mb-2"
          data-testid="folder-page-kind-label"
        >
          Folder
        </p>
        <h1 class="text-xl font-semibold text-base-content">
          {{ folderForView.folder.name }}
        </h1>
      </div>

      <WorkspaceIndexSettingsTabs
        v-model="activeTab"
        test-id-prefix="folder-workspace"
      />

      <div v-if="activeTab === 'index'" data-testid="folder-workspace-index">
        <ScopedIndexNoteEditor
          :notebook-id="folderForView.notebookRealm.notebook.id"
          :folder-id="folderForView.folder.id"
          :index-content="folderForView.indexContent ?? null"
          test-id-prefix="folder-index"
          rich-editor-scope-name="folder-index"
          heading-label="Folder index"
          flush
          @saved="refreshFolderPage"
        />
      </div>

      <FolderWorkspaceSettings
        v-else
        :folder-realm="folderForView"
        :fetch-folder-page="fetchFolderPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { FolderRealm } from "@generated/doughnut-backend-api"
import { computed, ref } from "vue"
import NotebookPageReadonlySummary from "@/components/notebook/NotebookPageReadonlySummary.vue"
import FolderWorkspaceSettings from "@/components/folder/FolderWorkspaceSettings.vue"
import ScopedIndexNoteEditor from "@/components/notebook/ScopedIndexNoteEditor.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import WorkspaceIndexSettingsTabs, {
  type WorkspaceIndexSettingsTab,
} from "@/components/commons/WorkspaceIndexSettingsTabs.vue"

const props = defineProps<{
  folderRealm: FolderRealm | undefined
  fetchFolderPage: () => Promise<void>
}>()

const folderForView = computed((): FolderRealm | undefined => {
  const r = props.folderRealm
  if (r?.notebookRealm?.notebook == null) return undefined
  return r
})

const activeTab = ref<WorkspaceIndexSettingsTab>("index")

const refreshFolderPage = () => props.fetchFolderPage()
</script>

<style scoped>
.folder-page-summary {
  background: color-mix(in oklch, var(--color-base-200) 80%, transparent);
  border-radius: 8px;
  margin-bottom: 1.5rem;
  padding: 1.5rem;
}
</style>
