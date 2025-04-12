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
    <button class="btn btn-primary btn-layout mt-2" @click="processForm">
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
    <NotebookAssistantManagementDialog :notebook="notebook" />
  </template>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import { useRouter } from "vue-router"
import type { Notebook, User } from "@/generated/backend"
import CheckInput from "@/components/form/CheckInput.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import TextInput from "../form/TextInput.vue"
import NotebookCertificateRequest from "./NotebookCertificateRequest.vue"
import NotebookAssistantManagementDialog from "./NotebookAssistantManagementDialog.vue"

const { managedApi } = useLoadingApi()
const router = useRouter()

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  user: { type: Object as PropType<User>, required: false },
})

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

const processForm = () => {
  managedApi.restNotebookController
    .update1(props.notebook.id, formData.value)
    .then(() => {
      router.go(0)
    })
    .catch((err) => {
      if (typeof err === "object" && err !== null) {
        errors.value = err as typeof errors.value
      } else {
        console.error("Unexpected error format:", err)
      }
    })
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

  try {
    await managedApi.restNotebookController.importObsidian(props.notebook.id, {
      file,
    })
    // Clear file input for reuse
    ;(event.target as HTMLInputElement).value = ""
    router.go(0) // Refresh page to show imported notes
  } catch (error) {
    alert("Failed to import file")
    console.error("Import error:", error)
  }
}
</script>
