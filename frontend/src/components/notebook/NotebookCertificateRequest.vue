<template>
  <h4>Request to obtain certificate from assessment</h4>
  <button id="request-approval-btn" :class="approvalButtonClasses" :disabled="isApprovalButtonDisabled" @click="requestNotebookApproval">
    {{ approvalButtonText }}
  </button>
</template>

<script setup lang="ts">
import { PropType, computed } from "vue"
import { useRouter } from "vue-router"
import { Notebook } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"

const { managedApi } = useLoadingApi()

const router = useRouter()

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})

const approvalButtonText = computed(() => {
  switch (props.notebook.approvalStatus) {
    case "NOT_APPROVED":
      return "Send Request"
    case "APPROVED":
      return "Certificate Request Approved"
    case "PENDING":
      return "Approval Pending"
    default:
      return "Send Request"
  }
})

const approvalButtonClasses = computed(() => {
  return {
    btn: true,
    "btn-primary": props.notebook.approvalStatus === "NOT_APPROVED",
    "btn-disabled":
      props.notebook.approvalStatus === "APPROVED" ||
      props.notebook.approvalStatus === "PENDING",
    "btn-layout": true,
    "mt-2": true,
    display: "block",
  }
})

const isApprovalButtonDisabled = computed(() => {
  return (
    props.notebook.approvalStatus === "APPROVED" ||
    props.notebook.approvalStatus === "PENDING"
  )
})
const requestNotebookApproval = () => {
  managedApi.restNotebookCertificateApprovalController
    .requestApprovalForNotebook(props.notebook.id)
    .then(() => {
      router.go(0)
    })
}
</script>

