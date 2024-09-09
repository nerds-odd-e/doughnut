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

<script lang="ts">
import { PropType, defineComponent } from "vue"
import { Notebook } from "@/generated/backend"
import CheckInput from "@/components/form/CheckInput.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import TextInput from "../form/TextInput.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  props: { notebook: { type: Object as PropType<Notebook>, required: true } },
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
      errors: {
        skipReviewEntirely: undefined as string | undefined,
        numberOfQuestionsInAssessment: undefined as string | undefined,
        certificateExpiry: undefined as string | undefined,
      },
    }
  },
  computed: {
    approvalButtonText() {
      switch (this.notebook.approvalStatus) {
        case "NOT_APPROVED":
          return "Send Request"
        case "APPROVED":
          return "Certificate Request Approved"
        case "PENDING":
          return "Approval Pending"
        default:
          return "Send Request"
      }
    },
    approvalButtonClasses() {
      return {
        btn: true,
        "btn-primary": this.notebook.approvalStatus === "NOT_APPROVED",
        "btn-disabled":
          this.notebook.approvalStatus === "APPROVED" ||
          this.notebook.approvalStatus === "PENDING",
        "btn-layout": true,
        "mt-2": true,
        display: "block",
      }
    },
    isApprovalButtonDisabled() {
      return (
        this.notebook.approvalStatus === "APPROVED" ||
        this.notebook.approvalStatus === "PENDING"
      )
    },
  },
  methods: {
    processForm() {
      this.managedApi.restNotebookController
        .update1(this.notebook.id, this.formData)
        .then(() => {
          this.$router.push({ name: "notebooks" })
        })
        .catch((err) => (this.errors = err))
    },
    requestNotebookApproval() {
      this.managedApi.restNotebookController
        .requestNotebookApproval(this.notebook.id)
        .then((response) => {
          this.notebook.approvalStatus = response.approvalStatus
        })
    },
  },
})
</script>
