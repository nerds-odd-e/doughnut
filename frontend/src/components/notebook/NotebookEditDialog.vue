<template>
  <h3>Edit notebook settings</h3>
  <div class="form-container">
    <CheckInput
      scope-name="notebook"
      field="skipReviewEntirely"
      v-model="formData.skipReviewEntirely"
      :errors="errors.skipReviewEntirely"
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
    <button hidden="true" id="request-approval-btn" class="btn btn-primary btn-layout mt-2 float-end" @click="requestNotebookApproval">
      Request approval
    </button>
    </div>
</template>

<script>
import CheckInput from "@/components/form/CheckInput.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import TextInput from "../form/TextInput.vue"

export default {
  setup() {
    return useLoadingApi()
  },
  props: { notebook: Object },
  components: { CheckInput, TextInput },
  data() {
    const {
      skipReviewEntirely,
      numberOfQuestionsInAssessment,
      certificateExpiry = "1y",
    } = this.notebook.notebookSettings
    return {
      formData: {
        skipReviewEntirely,
        numberOfQuestionsInAssessment,
        certificateExpiry,
      },
      errors: {},
    }
  },
  methods: {
    processForm() {
      this.managedApi.restNotebookController
        .update1(this.notebook.id, this.formData)
        .then(() => {
          this.$router.go()
        })
        .catch((err) => (this.errors = err))
    },
    requestNotebookApproval() {
      this.managedApi.restNotebookController
        .requestNotebookApproval(this.notebook.id)
        .then(() => {
          this.disableButton()
        })
    },
    disableButton() {
      const button = document.getElementById("request-approval-btn")
      button.disabled = true
      button.classList.replace("btn-primary", "btn-disabled")
    },
  },
}
</script>
