<template>
  <ContentLoader v-if="approvalState === undefined && !loaded" />
  <div v-else>
    <button :class="approvalButtonClasses" :disabled="isApprovalButtonDisabled" @click="requestNotebookApproval">
      {{ approvalButtonText }}
    </button>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, ref, watch } from "vue"
import type { Notebook, NotebookCertificateApproval } from "@generated/backend"
import { NotebookCertificateApprovalController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import ContentLoader from "@/components/commons/ContentLoader.vue"

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  approval: {
    type: Object as PropType<NotebookCertificateApproval | undefined>,
    required: false,
  },
  loaded: { type: Boolean, default: false },
})

const approvalState = ref<NotebookCertificateApproval | undefined>(
  props.approval
)

// Update approval when prop changes
watch(
  () => props.approval,
  (newApproval) => {
    approvalState.value = newApproval
  },
  { immediate: true }
)

const approvalButtonText = computed(() => {
  if (approvalState.value === undefined || approvalState.value === null) {
    return "Send Request"
  }
  if (approvalState.value.lastApprovalTime) {
    return "Certificate Request Approved"
  }
  return "Approval Pending"
})

const approvalButtonClasses = computed(() => ({
  "daisy-btn": true,
  "daisy-btn-sm": true,
  "daisy-btn-primary":
    approvalState.value === undefined || approvalState.value === null,
  "daisy-btn-outline":
    approvalState.value !== undefined && approvalState.value !== null,
  "daisy-btn-disabled":
    approvalState.value !== undefined && approvalState.value !== null,
}))

const isApprovalButtonDisabled = computed(
  () => approvalState.value !== undefined && approvalState.value !== null
)
const requestNotebookApproval = async () => {
  const { data: newApproval, error } = await apiCallWithLoading(() =>
    NotebookCertificateApprovalController.requestApprovalForNotebook({
      path: { notebook: props.notebook.id },
    })
  )
  if (!error) {
    approvalState.value = newApproval!
  }
}
</script>

