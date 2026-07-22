<template>
  <div class="container mx-auto pt-0 pb-4 max-w-6xl">
    <div class="notebook-page-summary mb-6" data-testid="notebook-page-summary">
      <p
        class="text-sm text-base-content/70 mb-2"
        data-testid="notebook-page-kind-label"
      >
        Notebook
      </p>
      <NotebookPageNameEditor
        :notebook-id="notebook.id"
        :name="notebook.name ?? ''"
        :settings-body="formData"
        @notebook-updated="(n) => emit('notebook-updated', n)"
      />
      <p v-if="notebook.description" class="notebook-page-summary-description">
        {{ notebook.description }}
      </p>
    </div>

    <WorkspaceReadmeSettingsTabs
      v-model="activeTab"
      test-id-prefix="notebook-workspace"
    />

    <div v-if="activeTab === 'readme'" data-testid="notebook-workspace-readme">
      <ScopedReadmeEditor
        :notebook-id="notebook.id"
        :readme-content="readmeContent"
        flush
        @saved="emit('readme-content-updated')"
      />
    </div>

    <NotebookWorkspaceSettings
      v-else
      :notebook="notebook"
      :user="user"
      :settings-body="formData"
      @notebook-updated="(n) => emit('notebook-updated', n)"
    />
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref, watch } from "vue"
import type { Notebook, User } from "@generated/doughnut-backend-api"
import NotebookPageNameEditor from "@/components/notebook/NotebookPageNameEditor.vue"
import NotebookWorkspaceSettings from "@/components/notebook/NotebookWorkspaceSettings.vue"
import ScopedReadmeEditor from "@/components/notebook/ScopedReadmeEditor.vue"
import WorkspaceReadmeSettingsTabs, {
  type WorkspaceReadmeSettingsTab,
} from "@/components/commons/WorkspaceReadmeSettingsTabs.vue"

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  user: { type: Object as PropType<User>, required: false },
  fetchNotebookPage: {
    type: Function as PropType<() => Promise<void>>,
    required: true,
  },
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

const activeTab = ref<WorkspaceReadmeSettingsTab>("readme")

const { skipMemoryTrackingEntirely } = props.notebook.notebookSettings

const formData = ref({
  skipMemoryTrackingEntirely,
  description: props.notebook.description ?? "",
})

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

.notebook-page-summary-description {
  color: color-mix(in oklch, var(--color-base-content) 60%, transparent);
  line-height: 1.6;
  margin-top: 0.5rem;
}
</style>
