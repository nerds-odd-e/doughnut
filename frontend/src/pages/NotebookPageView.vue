<template>
  <div class="container mx-auto pt-0 pb-4 max-w-6xl">
    <div class="notebook-page-summary mb-6" data-testid="notebook-page-summary">
      <p
        class="text-sm text-base-content/70 mb-2"
        data-testid="notebook-page-kind-label"
      >
        Notebook
      </p>
      <AutosavingPageNameEditor
        :name="notebook.name ?? ''"
        editor-data-test="notebook-page-name"
        empty-error-message="Notebook name cannot be empty"
        save-error-message="Failed to rename notebook"
        :persist-name="persistNotebookName"
      />
      <p
        class="text-sm text-base-content/80 m-0"
        data-testid="notebook-page-name-rename-warning"
      >
        {{ notebookRenameWikiLinkWarning }}
      </p>
    </div>

    <ReadmeSettingsTabs
      v-model="activeTab"
      test-id-prefix="notebook"
      include-health
    />

    <div v-if="activeTab === 'readme'" data-testid="notebook-readme">
      <ScopedReadmeEditor
        :notebook-id="notebook.id"
        :readme-content="readmeContent"
        flush
        @saved="emit('readme-content-updated')"
      />
    </div>

    <NotebookSettings
      v-else-if="activeTab === 'settings'"
      :notebook="notebook"
      :user="user"
      :settings-body="formData"
      @notebook-updated="(n) => emit('notebook-updated', n)"
    />

    <NotebookHealthPanel
      v-else-if="activeTab === 'health'"
      :notebook-id="notebook.id"
    />
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref, watch } from "vue"
import type { Notebook, User } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebookHealthPanel from "@/components/notebook/NotebookHealthPanel.vue"
import AutosavingPageNameEditor from "@/components/commons/AutosavingPageNameEditor.vue"
import NotebookSettings from "@/components/notebook/NotebookSettings.vue"
import ScopedReadmeEditor from "@/components/notebook/ScopedReadmeEditor.vue"
import ReadmeSettingsTabs, {
  type ReadmeSettingsTab,
} from "@/components/commons/ReadmeSettingsTabs.vue"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  user: { type: Object as PropType<User>, required: false },
  readmeContent: {
    type: String as PropType<string | null>,
    required: false,
    default: null,
  },
})

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
  (e: "readme-content-updated"): void
}>()

const activeTab = ref<ReadmeSettingsTab>("readme")

const notebookRenameWikiLinkWarning =
  "If you change this notebook's name, wiki links from other notebooks to notes here may stop working."

const { skipMemoryTrackingEntirely } = props.notebook.notebookSettings

const formData = ref({
  skipMemoryTrackingEntirely,
  description: props.notebook.description ?? "",
})

const persistNotebookName = async (name: string) => {
  const { data: updatedNotebook, error } = await apiCallWithLoading(() =>
    NotebookController.updateNotebook({
      path: { notebook: props.notebook.id },
      body: { ...formData.value, name },
    })
  )
  if (error) throw error
  emit("notebook-updated", updatedNotebook!)
}

watch(
  () => props.notebook,
  (nb) => {
    formData.value = {
      skipMemoryTrackingEntirely:
        nb.notebookSettings.skipMemoryTrackingEntirely ?? false,
      description: nb.description ?? "",
    }
  },
  { deep: true }
)
</script>

<style scoped>
.notebook-page-summary {
  background: color-mix(in oklch, var(--color-base-200) 80%, transparent);
  border-radius: 8px;
  margin-bottom: 1.5rem;
  padding: 1.5rem;
}
</style>
