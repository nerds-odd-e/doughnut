<template>
  <h3>Edit notebook settings</h3>
  <div class="form-container">
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
  </div>
  <hr/>
  <div>
    <NotebookCertificateRequest v-bind="{ notebook }" />
  </div>

  <!-- New Admin Section -->
  <template v-if="user?.admin">
    <hr/>
    <h3>Notebook Assistant Management</h3>
    <div class="form-container">
      <TextInput v-model="additionalInstruction" field="additionalInstruction" />
      <div class="mt-2">
        <button class="btn btn-secondary me-2" @click.prevent="createAssistantForNotebook">
          Create Assistant For Notebook
        </button>
        <button class="btn btn-secondary" @click.prevent="downloadNotebookDump">
          Download Notebook Dump
        </button>
      </div>
    </div>
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
import { saveAs } from "file-saver"

const { managedApi } = useLoadingApi()
const router = useRouter()

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  user: { type: Object as PropType<User>, required: false },
})

// Original form data
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

// Assistant management data
const additionalInstruction = ref("")

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

const createAssistantForNotebook = async () => {
  await managedApi.restAiAssistantCreationController.recreateNotebookAssistant(
    props.notebook.id,
    {
      additionalInstruction: additionalInstruction.value,
    }
  )
}

const downloadNotebookDump = async () => {
  const notes = await managedApi.restNotebookController.downloadNotebookDump(
    props.notebook.id
  )
  const blob = new Blob([JSON.stringify(notes, null, 2)], {
    type: "application/json",
  })
  saveAs(blob, "notebook-dump.json")
}
</script>
