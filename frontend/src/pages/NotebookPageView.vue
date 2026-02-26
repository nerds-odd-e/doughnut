<template>
  <GlobalBar>
    <button
      class="daisy-btn daisy-btn-sm daisy-btn-ghost"
      @click="goToNotebooks"
    >
      Back to Notebooks
    </button>
  </GlobalBar>
  <div class="daisy-container daisy-mx-auto daisy-p-4 daisy-max-w-6xl">
    <!-- Head Note Section -->
    <div class="notebook-head-note-wrapper daisy-mb-6">
      <div class="daisy-text-sm daisy-text-base-content/60">
        Head note of notebook:
      </div>
      <router-link
        :to="{ name: 'noteShow', params: { noteId: notebook.headNoteId } }"
        class="no-underline"
      >
        <div>
          <h5 class="daisy-text-lg daisy-font-semibold">
            {{ notebook.title }}
          </h5>
          <p v-if="notebook.shortDetails" class="note-short-details">
            {{ notebook.shortDetails }}
          </p>
        </div>
      </router-link>
    </div>
    
    <!-- Notebook Management Section -->
    <section class="settings-section daisy-mb-6">
      <div class="section-header">
        <h4 class="section-title">Notebook Management</h4>
        <p class="section-description">
          Manage your notebook's organization and sharing settings.
        </p>
      </div>
      <div class="daisy-flex daisy-flex-wrap daisy-gap-2">
        <PopButton
          title="Move to ..."
          v-if="user?.externalIdentifier === notebook.creatorId"
          btnClass="daisy-btn daisy-btn-outline daisy-btn-sm"
        >
          <template #button_face>
            <div class="daisy-flex daisy-items-center daisy-gap-2">
              <SvgMoveToCircle />
              <span>Move to ...</span>
            </div>
          </template>
          <NotebookMoveDialog v-bind="{ notebook }" />
        </PopButton>
        <button
          class="daisy-btn daisy-btn-outline daisy-btn-sm"
          @click="shareNotebook()"
          title="Share notebook to bazaar"
        >
          <div class="daisy-flex daisy-items-center daisy-gap-2">
            <SvgBazaarShare />
            <span>Share notebook to bazaar</span>
          </div>
        </button>
      </div>
    </section>
    
    <!-- Notebook Settings Section -->
    <section class="settings-section daisy-mb-6">
      <div class="section-header">
        <h4 class="section-title">Notebook Settings</h4>
        <p class="section-description">
          Configure memory tracking, assessment, and certificate settings for this notebook.
        </p>
      </div>
      <div class="settings-grid">
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
        <div class="settings-item">
          <TextInput
            scope-name="notebook"
            field="numberOfQuestionsInAssessment"
            title="Number of Questions in Assessment"
            v-model="formData.numberOfQuestionsInAssessment"
            :error-message="errors.numberOfQuestionsInAssessment"
          />
          <p class="field-hint">
            The number of questions to include in certificate assessments for this notebook.
          </p>
        </div>
        <div class="settings-item">
          <TextInput
            scope-name="notebook"
            field="certificateExpiry"
            title="Certificate Expiry"
            hint="Format: 1y 1m 1d"
            v-model="formData.certificateExpiry"
            :error-message="errors.certificateExpiry"
          />
          <p class="field-hint">
            How long certificates issued for this notebook remain valid. Use format like "1y" for 1 year, "6m" for 6 months, or "1y 2m 3d" for combined periods.
          </p>
        </div>
      </div>
      <button class="daisy-btn daisy-btn-primary daisy-btn-sm daisy-mt-4" @click="processForm">
        Update Settings
      </button>
    </section>

    <!-- Certificate Request Section -->
    <section class="settings-section daisy-mb-6">
      <div class="section-header">
        <h4 class="section-title">Certificate Request</h4>
        <p class="section-description">
          Request approval to obtain a certificate from assessment completion.
        </p>
      </div>
      <NotebookCertificateRequest 
        v-bind="{ notebook, approval, loaded: approvalLoaded }" 
      />
    </section>

    <!-- Obsidian Integration Section -->
    <section class="settings-section daisy-mb-6">
      <div class="section-header">
        <h4 class="section-title">Obsidian Integration</h4>
        <p class="section-description">
          Import notes from Obsidian vaults or export this notebook for use in Obsidian.
        </p>
      </div>
      <div class="daisy-flex daisy-flex-wrap daisy-gap-2">
        <label class="daisy-btn daisy-btn-outline daisy-btn-sm">
          Import from Obsidian
          <input
            type="file"
            accept=".zip"
            class="!hidden"
            style="display: none !important"
            @change="handleObsidianImport"
          />
        </label>
        <button class="daisy-btn daisy-btn-outline daisy-btn-sm" @click="exportForObsidian">
          Export for Obsidian
        </button>
      </div>
    </section>

    <section class="settings-section daisy-mb-6">
      <div class="section-header">
        <h4 class="section-title">Assistant Management</h4>
        <p class="section-description">
          Configure AI assistant settings and instructions for this notebook.
        </p>
      </div>
      <NotebookAssistantManagementDialog 
        :notebook="notebook" 
        :additional-instructions="additionalInstructions"
      />
    </section>

    <!-- Notebook Indexing Section -->
    <section class="settings-section daisy-mb-6">
      <div class="section-header">
        <h4 class="section-title">Notebook Indexing</h4>
        <p class="section-description">
          Manage the search index for this notebook. Reset to rebuild from scratch, or update to index new content.
        </p>
      </div>
      <div class="daisy-flex daisy-flex-wrap daisy-gap-2">
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
import { ref } from "vue"
import { useRouter } from "vue-router"
import type { Notebook, User } from "@generated/backend"
import { NotebookController } from "@generated/backend/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useToast } from "@/composables/useToast"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import SvgBazaarShare from "@/components/svgs/SvgBazaarShare.vue"
import SvgMoveToCircle from "@/components/svgs/SvgMoveToCircle.vue"
import NotebookMoveDialog from "@/components/notebook/NotebookMoveDialog.vue"
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
const { popups } = usePopups()

const goToNotebooks = () => {
  router.push({ name: "notebooks" })
}

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

<style scoped>
.notebook-head-note-wrapper {
  background: oklch(var(--b2) / 0.8);
  border-radius: 8px;
  margin-bottom: 1.5rem;
  padding: 1.5rem;
}

.note-short-details {
  color: oklch(var(--bc) / 0.6);
  line-height: 1.6;
  margin-top: 0.5rem;
}

.settings-section {
  background: oklch(var(--b1));
  border: 1px solid oklch(var(--b3));
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
  color: oklch(var(--bc));
}

.section-description {
  font-size: 0.875rem;
  color: oklch(var(--bc) / 0.7);
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

.field-hint {
  font-size: 0.75rem;
  color: oklch(var(--bc) / 0.6);
  line-height: 1.4;
  margin-top: 0.25rem;
}
</style>

