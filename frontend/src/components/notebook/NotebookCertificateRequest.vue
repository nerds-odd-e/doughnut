<template>
  <h4 class="daisy-text-lg">Request to obtain certificate from assessment</h4>
  <ContentLoader v-if="loaded === false" />
  <div v-else>
    <button :class="approvalButtonClasses" :disabled="isApprovalButtonDisabled" @click="requestNotebookApproval">
      {{ approvalButtonText }}
    </button>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, onMounted, ref } from "vue"
import type { Notebook, NotebookCertificateApproval } from "@generated/backend"
import { NotebookCertificateApprovalController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})

const loaded = ref(false)
const approval = ref<NotebookCertificateApproval | undefined>()

onMounted(async () => {
  const { data: dto, error } =
    await NotebookCertificateApprovalController.getApprovalForNotebook({
      path: { notebook: props.notebook.id },
    })
  if (!error) {
    approval.value = dto!.approval
    loaded.value = true
  }
})

const approvalButtonText = computed(() => {
  if (approval.value === undefined || approval.value === null) {
    return "Send Request"
  }
  if (approval.value.lastApprovalTime) {
    return "Certificate Request Approved"
  }
  return "Approval Pending"
})

const approvalButtonClasses = computed(() => {
  return {
    "daisy-btn": true,
    "daisy-btn-primary":
      approval.value === undefined || approval.value === null,
    "daisy-btn-disabled":
      approval.value !== undefined && approval.value !== null,
    "daisy-mt-2": true,
    display: "block",
  }
})

const isApprovalButtonDisabled = computed(() => {
  return approval.value !== undefined && approval.value !== null
})
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

