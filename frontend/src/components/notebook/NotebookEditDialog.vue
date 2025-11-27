<template>
  <h3>Edit notebook settings</h3>
    <CheckInput
      scope-name="notebook"
      field="skipMemoryTrackingEntirely"
      v-model="formData.skipMemoryTrackingEntirely"
      :error-message="errors.skipMemoryTrackingEntirely"
    />
    <TextInput
      scope-name="notebook"
      field="numberOfQuestionsInAssessment"
      v-model="formData.numberOfQuestionsInAssessment"
      :error-message="errors.numberOfQuestionsInAssessment"
    />
    <TextInput
      scope-name="notebook"
      field="certificateExpiry"
      hint="Format (1y 1m 1d)"
      v-model="formData.certificateExpiry"
      :error-message="errors.certificateExpiry"
    />
    <button class="daisy-btn daisy-btn-primary daisy-mt-2" @click="processForm">
      Update
    </button>
  <hr/>
  <div>
    <NotebookCertificateRequest v-bind="{ notebook }" />
  </div>

  <!-- Obsidian Import/Export Section -->
  <div class="daisy-flex daisy-flex-col daisy-gap-2 daisy-my-4">
    <h4 class="daisy-text-lg">Obsidian Integration</h4>
    <div class="daisy-flex daisy-gap-2">
      <label class="daisy-btn daisy-btn-sm daisy-btn-outline">
        Import from Obsidian
        <input
          type="file"
          accept=".zip"
          class="!hidden"
          style="display: none !important"
          @change="handleObsidianImport"
        />
      </label>
      <button class="daisy-btn daisy-btn-sm daisy-btn-outline" @click="exportForObsidian">
        Export for Obsidian
      </button>
    </div>
  </div>

  <!-- Admin Section -->
  <template v-if="user?.admin">
    <hr/>
    <NotebookAssistantManagementDialog :notebook="notebook" :closer="closer" />
  </template>

  <!-- Notebook Indexing Section -->
  <div class="daisy-mt-4">
    <h4 class="daisy-text-lg daisy-mb-2">Notebook Indexing</h4>
    <div class="daisy-flex daisy-gap-2">
      <button
        class="daisy-btn daisy-btn-secondary"
        @click="reindexNotebook"
        :disabled="isIndexing"
      >
        <span v-if="isIndexing">Working...</span>
        <span v-else>Reset notebook index</span>
      </button>
      <button
        class="daisy-btn daisy-btn-secondary"
        @click="updateIndexNotebook"
        :disabled="isIndexing"
      >
        <span v-if="isIndexing">Working...</span>
        <span v-else>Update index</span>
      </button>
    </div>
  </div>

  <!-- Indexing Complete Confirmation Dialog -->
  <div v-if="showIndexingComplete" class="daisy-modal daisy-modal-open">
    <div class="daisy-modal-box">
      <h3 class="daisy-font-bold daisy-text-lg">Index operation complete</h3>
      <p class="daisy-py-4">Notebook index has been updated.</p>
      <div class="daisy-modal-action">
        <button class="daisy-btn daisy-btn-primary" @click="closeIndexingComplete">
          OK
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { Notebook, User } from "@generated/backend"
import { NotebookController } from "@generated/backend/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import CheckInput from "@/components/form/CheckInput.vue"
import TextInput from "../form/TextInput.vue"
import NotebookCertificateRequest from "./NotebookCertificateRequest.vue"
import NotebookAssistantManagementDialog from "./NotebookAssistantManagementDialog.vue"

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  user: { type: Object as PropType<User>, required: false },
  closer: { type: Function as PropType<() => void>, required: false },
})

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
}>()

// Form data
const {
  skipMemoryTrackingEntirely,
  numberOfQuestionsInAssessment,
  certificateExpiry = "1y",
} = props.notebook.notebookSettings

const formData = ref({
  skipMemoryTrackingEntirely,
  numberOfQuestionsInAssessment,
  certificateExpiry,
})

const errors = ref({
  skipMemoryTrackingEntirely: undefined as string | undefined,
  numberOfQuestionsInAssessment: undefined as string | undefined,
  certificateExpiry: undefined as string | undefined,
})

// Indexing state
const isIndexing = ref(false)
const showIndexingComplete = ref(false)

const processForm = async () => {
  const { data: updatedNotebook, error } = await apiCallWithLoading(() =>
    NotebookController.updateNotebook({
      path: { notebook: props.notebook.id },
      body: formData.value,
    })
  )
  if (!error) {
    emit("notebook-updated", updatedNotebook!)
    props.closer?.()
  } else {
    // Error is handled by global interceptor (toast notification)
    // Extract field-level errors if available (for 400 validation errors)
    const errorObj = toOpenApiError(error)
    errors.value = { ...errors.value, ...(errorObj.errors || {}) }
  }
}

const exportForObsidian = () => {
  const link = document.createElement("a")
  link.style.display = "none"
  link.href = `/api/notebooks/${props.notebook.id}/obsidian`
  link.download = `${props.notebook.title}-obsidian.zip`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const handleObsidianImport = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  const { error } = await apiCallWithLoading(() =>
    NotebookController.importObsidian({
      path: { notebook: props.notebook.id },
      body: { file },
    })
  )
  if (!error) {
    // Clear file input for reuse
    ;(event.target as HTMLInputElement).value = ""
    // Obsidian import doesn't return updated notebook, so refresh the page
    window.location.reload()
  } else {
    // Error is handled by global interceptor (toast notification)
    alert("Failed to import file")
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
    showIndexingComplete.value = true
  } else {
    // Error is handled by global interceptor (toast notification)
    alert("Failed to reset notebook index")
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
    showIndexingComplete.value = true
  } else {
    // Error is handled by global interceptor (toast notification)
    alert("Failed to update notebook index")
  }
  isIndexing.value = false
}

const closeIndexingComplete = () => {
  showIndexingComplete.value = false
}
</script>
