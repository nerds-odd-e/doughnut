<template>
  <div class="container mx-auto py-4 max-w-6xl">
    <div class="notebook-page-summary mb-6" data-testid="notebook-page-summary">
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

    <ScopedIndexNoteEditor
      :notebook-id="notebook.id"
      :index-content="indexContent"
      @saved="emit('index-content-updated')"
    />

    <NotebookAttachedBookSection :notebook-id="notebook.id" />

    <!-- Notebook Management Section -->
    <section class="settings-section mb-6">
      <div class="section-header">
        <h4 class="section-title">Notebook Management</h4>
        <p class="section-description">
          Manage your notebook's organization and sharing settings.
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <PopButton
          title="Move to ..."
          v-if="user?.externalIdentifier === notebook.creatorId"
          btnClass="daisy-btn daisy-btn-outline daisy-btn-sm"
        >
          <template #button_face>
            <div class="flex items-center gap-2">
              <GitMerge class="w-6 h-6" />
              <span>Move to ...</span>
            </div>
          </template>
          <NotebookMoveForm v-bind="{ notebook }" />
        </PopButton>
        <button
          class="daisy-btn daisy-btn-outline daisy-btn-sm"
          @click="shareNotebook()"
          title="Share notebook to bazaar"
        >
          <div class="flex items-center gap-2">
            <Share2 class="w-6 h-6" />
            <span>Share notebook to bazaar</span>
          </div>
        </button>
      </div>
    </section>
    
    <!-- Notebook Settings Section -->
    <section class="settings-section mb-6">
      <div class="section-header">
        <h4 class="section-title">Notebook Settings</h4>
        <p class="section-description">
          Configure memory tracking and an optional short plain-text message
          for this notebook.
        </p>
      </div>
      <div class="settings-grid">
        <div class="settings-item settings-item-full-width">
          <TextArea
            scope-name="notebook"
            field="description"
            v-model="formData.description"
            :rows="3"
            placeholder="Optional short plain-text message (shown on notebook cards)"
          />
          <p class="field-hint">
            Plain text only, up to 500 characters. This is separate from notebook page body
            content.
          </p>
        </div>
        <div class="settings-item">
          <CheckInput
            scope-name="notebook"
            field="skipMemoryTrackingEntirely"
            title="Skip Memory Tracking"
            v-model="formData.skipMemoryTrackingEntirely"
            :error-message="errors.skipMemoryTrackingEntirely"
          />
          <p class="field-hint">
            When enabled, notes in this notebook will not be included in memory tracking and recall sessions.
          </p>
        </div>
      </div>
      <button class="daisy-btn daisy-btn-primary daisy-btn-sm mt-4" @click="processForm">
        Update Settings
      </button>
    </section>

    <!-- Notebook Indexing Section -->
    <section class="settings-section mb-6">
      <div class="section-header">
        <h4 class="section-title">Notebook Indexing</h4>
        <p class="section-description">
          Manage the search index for this notebook. Reset to rebuild from scratch, or update to index new content.
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <button
          class="daisy-btn daisy-btn-outline daisy-btn-sm"
          @click="reindexNotebook"
          :disabled="isIndexing"
        >
          <span v-if="isIndexing">Working...</span>
          <span v-else>Reset notebook index</span>
        </button>
        <button
          class="daisy-btn daisy-btn-outline daisy-btn-sm"
          @click="updateIndexNotebook"
          :disabled="isIndexing"
        >
          <span v-if="isIndexing">Working...</span>
          <span v-else>Update index</span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref, watch } from "vue"
import { useRouter } from "vue-router"
import type { Notebook, User } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useToast } from "@/composables/useToast"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { GitMerge, Share2 } from "@lucide/vue"
import NotebookMoveForm from "@/components/notebook/NotebookMoveForm.vue"
import CheckInput from "@/components/form/CheckInput.vue"
import TextArea from "@/components/form/TextArea.vue"
import NotebookAttachedBookSection from "@/components/notebook/NotebookAttachedBookSection.vue"
import NotebookPageNameEditor from "@/components/notebook/NotebookPageNameEditor.vue"
import ScopedIndexNoteEditor from "@/components/notebook/ScopedIndexNoteEditor.vue"

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

const { showSuccessToast } = useToast()
const router = useRouter()
const { popups } = usePopups()

const shareNotebook = async () => {
  if (await popups.confirm(`Confirm to share?`)) {
    const { error } = await apiCallWithLoading(() =>
      NotebookController.shareNotebook({
        path: { notebook: props.notebook.id },
      })
    )
    if (!error) {
      await router.push({ name: "notebooks" })
    }
  }
}

// Form data
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

const errors = ref({
  skipMemoryTrackingEntirely: undefined as string | undefined,
})

// Indexing state
const isIndexing = ref(false)

const processForm = async () => {
  const { data: updatedNotebook, error } = await apiCallWithLoading(() =>
    NotebookController.updateNotebook({
      path: { notebook: props.notebook.id },
      body: formData.value,
    })
  )
  if (!error) {
    emit("notebook-updated", updatedNotebook!)
    showSuccessToast("Notebook settings updated successfully")
  } else {
    // Error is handled by global interceptor (toast notification)
    // Extract field-level errors if available (for 400 validation errors)
    const errorObj = toOpenApiError(error)
    errors.value = { ...errors.value, ...(errorObj.errors || {}) }
  }
}

const reindexNotebook = async () => {
  isIndexing.value = true
  const { error } = await apiCallWithLoading(() =>
    NotebookController.resetNotebookIndex({
      path: { notebook: props.notebook.id },
    })
  )
  if (!error) {
    showSuccessToast("Notebook index reset successfully")
  } else {
    // Error is handled by global interceptor (toast notification)
  }
  isIndexing.value = false
}

const updateIndexNotebook = async () => {
  isIndexing.value = true
  const { error } = await apiCallWithLoading(() =>
    NotebookController.updateNotebookIndex({
      path: { notebook: props.notebook.id },
    })
  )
  if (!error) {
    showSuccessToast("Notebook index updated successfully")
  } else {
    // Error is handled by global interceptor (toast notification)
  }
  isIndexing.value = false
}
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

.settings-section {
  background: var(--color-base-100);
  border: 1px solid var(--color-base-300);
  border-radius: 8px;
  padding: 1.5rem;
}

.section-header {
  margin-bottom: 1.25rem;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: var(--color-base-content);
}

.section-description {
  font-size: 0.875rem;
  color: color-mix(in oklch, var(--color-base-content) 70%, transparent);
  line-height: 1.5;
}

.settings-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
}

@media (min-width: 768px) {
  .settings-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.settings-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.settings-item-full-width {
  grid-column: 1 / -1;
}

.field-hint {
  font-size: 0.75rem;
  color: color-mix(in oklch, var(--color-base-content) 60%, transparent);
  line-height: 1.4;
  margin-top: 0.25rem;
}
</style>

