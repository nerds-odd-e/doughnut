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
  </div>
  <hr/>
  <div>
    <h4>Request to obtain certificate from assessment</h4>
    <button id="request-approval-btn" :class="approvalButtonClasses" :disabled="isApprovalButtonDisabled" @click="requestNotebookApproval">
      {{ approvalButtonText }}
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
  props: { notebook: { type: Object, required: true } },
  components: { CheckInput, TextInput },
  data() {
    const {
      skipReviewEntirely = false,
      numberOfQuestionsInAssessment = 0,
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
  computed: {
    approvalButtonText() {
      if (
        this.notebook.last_approval_time &&
        this.notebook.last_approval_time > this.notebook.updated_at
      ) {
        return "Certificate Request Approved"
      }

      switch (this.notebook.approvalStatus) {
        case "PENDING":
          return "Approval Pending"
        default:
          return "Send Request"
      }
    },
    approvalButtonClasses() {
      return {
        btn: true,
        "btn-primary":
          this.notebook.approvalStatus === "NOT_APPROVED" ||
          (this.notebook.last_approval_time < this.notebook.updated_at &&
            this.notebook.approvalStatus !== "PENDING"),
        "btn-disabled":
          this.notebook.last_approval_time > this.notebook.updated_at ||
          this.notebook.approvalStatus === "PENDING",
        "btn-layout": true,
        "mt-2": true,
        display: "block",
      }
    },
    isApprovalButtonDisabled() {
      return (
        (this.notebook.last_approval_time &&
          this.notebook.last_approval_time >= this.notebook.updated_at) ||
        this.notebook.approvalStatus === "PENDING"
      )
    },
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
        .requestNotebookApproval(this.notebook?.id)
        .then((response) => {
          this.notebook.approvalStatus = response.approvalStatus
        })
    },
  },
}
</script>
