<template>
  <GlobalBar>
    <button
      class="daisy-btn daisy-btn-sm daisy-btn-ghost"
      @click="goToNotebooks"
    >
      Back to Notebooks
    </button>
  </GlobalBar>
  <div class="daisy-container daisy-mx-auto daisy-p-4">
    <NotebookCard :notebook="notebook" />
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
      <NotebookCertificateRequest 
        v-bind="{ notebook, approval, loaded: approvalLoaded }" 
      />
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
      <NotebookAssistantManagementDialog 
        :notebook="notebook" 
        :additional-instructions="additionalInstructions"
      />
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
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import { useRouter } from "vue-router"
import type { Notebook, User } from "@generated/backend"
import { NotebookController } from "@generated/backend/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useToast } from "@/composables/useToast"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import NotebookCard from "@/components/notebooks/NotebookCard.vue"
import CheckInput from "@/components/form/CheckInput.vue"
import TextInput from "@/components/form/TextInput.vue"
import NotebookCertificateRequest from "@/components/notebook/NotebookCertificateRequest.vue"
import NotebookAssistantManagementDialog from "@/components/notebook/NotebookAssistantManagementDialog.vue"

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  user: { type: Object as PropType<User>, required: false },
  approval: {
    type: Object as PropType<
      import("@generated/backend").NotebookCertificateApproval | undefined
    >,
    required: false,
  },
  approvalLoaded: { type: Boolean, default: false },
  additionalInstructions: { type: String, default: "" },
})

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
}>()

const { showSuccessToast } = useToast()
const router = useRouter()

const goToNotebooks = () => {
  router.push({ name: "notebooks" })
}

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

const exportForObsidian = () => {
  const link = document.createElement("a")
  link.style.display = "none"
  link.href = `/api/notebooks/${props.notebook.id}/obsidian`
  link.download = `${props.notebook.title}-obsidian.zip`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  showSuccessToast("Export started")
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
    showSuccessToast("Obsidian import completed successfully")
    // Reload the page to show updated content
    window.location.reload()
  } else {
    // Error is handled by global interceptor (toast notification)
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

