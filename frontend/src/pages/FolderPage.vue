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
        <AutosavingPageNameEditor
          :name="folderForView.folder.name"
          editor-data-test="folder-page-name"
          empty-error-message="Folder name cannot be empty. Enter a name to rename this folder."
          save-error-message="Failed to rename folder"
          :persist-name="persistFolderName"
        />
      </div>

      <ReadmeSettingsTabs
        v-model="activeTab"
        test-id-prefix="folder"
      />

      <div v-if="activeTab === 'readme'" data-testid="folder-readme">
        <ScopedReadmeEditor
          :notebook-id="folderForView.notebookRealm.notebook.id"
          :folder-id="folderForView.folder.id"
          :readme-content="folderForView.readmeContent ?? null"
          test-id-prefix="folder-readme"
          rich-editor-scope-name="folder-readme"
          flush
          @saved="refreshFolderPage"
        />
      </div>

      <FolderSettings
        v-else
        :folder-realm="folderForView"
        :fetch-folder-page="fetchFolderPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { FolderRealm } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, ref } from "vue"
import NotebookPageReadonlySummary from "@/components/notebook/NotebookPageReadonlySummary.vue"
import FolderSettings from "@/components/folder/FolderSettings.vue"
import AutosavingPageNameEditor from "@/components/commons/AutosavingPageNameEditor.vue"
import ScopedReadmeEditor from "@/components/notebook/ScopedReadmeEditor.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import ReadmeSettingsTabs, {
  type ReadmeSettingsTab,
} from "@/components/commons/ReadmeSettingsTabs.vue"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const props = defineProps<{
  folderRealm: FolderRealm | undefined
  fetchFolderPage: () => Promise<void>
}>()

const folderForView = computed((): FolderRealm | undefined => {
  const r = props.folderRealm
  if (r?.notebookRealm?.notebook == null) return undefined
  return r
})

const activeTab = ref<ReadmeSettingsTab>("readme")

const persistFolderName = async (name: string) => {
  const folderRealm = folderForView.value!
  const { error } = await apiCallWithLoading(() =>
    NotebookController.renameFolder({
      path: {
        notebook: folderRealm.notebookRealm.notebook.id,
        folder: folderRealm.folder.id,
      },
      body: { name },
    })
  )
  if (error) throw error
  refreshSidebarStructuralListings()
  await props.fetchFolderPage()
}

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
