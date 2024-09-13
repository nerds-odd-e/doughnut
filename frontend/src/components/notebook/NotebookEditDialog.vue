<template>
  <h3>Edit notebook settings</h3>
  <div class="form-container">
    <CheckInput
      scope-name="notebook"
      field="skipReviewEntirely"
      v-model="formData.skipReviewEntirely"
      :errors="getErrorObject('skipReviewEntirely')"
    />
    <TextInput
      scope-name="notebook"
      field="numberOfQuestionsInAssessment"
      v-model="formData.numberOfQuestionsInAssessment"
      :errors="errors.numberOfQuestionsInAssessment"
    />
    <TextInput
      scope-name="notebook"
      field="certificateExpiry"
      hint="Format (1y 1m 1d)"
      v-model="formData.certificateExpiry"
      :errors="errors.certificateExpiry"
    />
    <button class="btn btn-primary btn-layout mt-2" @click="processForm">
      Update
    </button>
  </div>
  <hr/>
  <div>
    <NotebookCertificateRequest v-bind="{ notebook }" />
  </div>
</template>

<script setup lang="ts">
import { PropType, ref, computed } from "vue"
import { useRouter } from "vue-router"
import { Notebook } from "@/generated/backend"
import CheckInput from "@/components/form/CheckInput.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import TextInput from "../form/TextInput.vue"
import NotebookCertificateRequest from "./NotebookCertificateRequest.vue"

const { managedApi } = useLoadingApi()
const router = useRouter()

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})

const {
  skipReviewEntirely,
  numberOfQuestionsInAssessment,
  certificateExpiry = "1y",
} = props.notebook.notebookSettings

const formData = ref({
  skipReviewEntirely,
  numberOfQuestionsInAssessment,
  certificateExpiry,
})

const errors = ref({
  skipReviewEntirely: undefined as string | undefined,
  numberOfQuestionsInAssessment: undefined as string | undefined,
  certificateExpiry: undefined as string | undefined,
})

const getErrorObject = computed(() => (field: "skipReviewEntirely") => {
  const error = errors.value[field]
  return error ? { [field]: error } : undefined
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
</script>