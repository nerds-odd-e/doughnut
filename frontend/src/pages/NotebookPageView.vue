<template>
  <div class="container mx-auto py-4 max-w-6xl">
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

    <div
      class="daisy-tabs daisy-tabs-box bg-base-200 p-2 mb-6"
      data-testid="notebook-workspace-tabs"
    >
      <a
        :class="tabClass('home')"
        role="button"
        href="#"
        data-testid="notebook-workspace-tab-home"
        @click.prevent="activeTab = 'home'"
      >Home</a>
      <a
        :class="tabClass('settings')"
        role="button"
        href="#"
        data-testid="notebook-workspace-tab-settings"
        @click.prevent="activeTab = 'settings'"
      >Settings</a>
    </div>

    <div v-if="activeTab === 'home'" data-testid="notebook-workspace-home">
      <ScopedIndexNoteEditor
        :notebook-id="notebook.id"
        :index-content="indexContent"
        @saved="emit('index-content-updated')"
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
import ScopedIndexNoteEditor from "@/components/notebook/ScopedIndexNoteEditor.vue"

type WorkspaceTab = "home" | "settings"

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  user: { type: Object as PropType<User>, required: false },
  fetchNotebookPage: {
    type: Function as PropType<() => Promise<void>>,
    required: true,
  },
  indexContent: {
    type: String as PropType<string | null>,
    required: false,
    default: null,
  },
})

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
  (e: "index-content-updated"): void
}>()

const activeTab = ref<WorkspaceTab>("home")

const tabClass = (tab: WorkspaceTab) =>
  `daisy-tab daisy-tab-lg ${activeTab.value === tab ? "daisy-tab-active" : ""}`

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
