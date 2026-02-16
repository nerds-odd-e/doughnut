<template>
  <ContentLoader v-if="approval === undefined && !loaded" />
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

const approval = ref<NotebookCertificateApproval | undefined>(props.approval)

// Update approval when prop changes
watch(
  () => props.approval,
  (newApproval) => {
    approval.value = newApproval
  },
  { immediate: true }
)

const approvalButtonText = computed(() => {
  if (approval.value === undefined || approval.value === null) {
    return "Send Request"
  }
  if (approval.value.lastApprovalTime) {
    return "Certificate Request Approved"
  }
  return "Approval Pending"
})

const approvalButtonClasses = computed(() => ({
  "daisy-btn": true,
  "daisy-btn-sm": true,
  "daisy-btn-primary": approval.value === undefined || approval.value === null,
  "daisy-btn-outline": approval.value !== undefined && approval.value !== null,
  "daisy-btn-disabled": approval.value !== undefined && approval.value !== null,
}))

const isApprovalButtonDisabled = computed(
  () => approval.value !== undefined && approval.value !== null
)
const requestNotebookApproval = async () => {
  const { data: newApproval, error } = await apiCallWithLoading(() =>
    NotebookCertificateApprovalController.requestApprovalForNotebook({
      path: { notebook: props.notebook.id },
    })
  )
  if (!error) {
    approval.value = newApproval!
  }
}
</script>

