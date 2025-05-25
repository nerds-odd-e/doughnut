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
import type { Notebook, NotebookCertificateApproval } from "generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"

const { managedApi } = useLoadingApi()

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})

const loaded = ref(false)
const approval = ref<NotebookCertificateApproval | undefined>()

onMounted(async () => {
  approval.value =
    await managedApi.restNotebookCertificateApprovalController.getApprovalForNotebook(
      props.notebook.id
    )
  loaded.value = true
})

const approvalButtonText = computed(() => {
  if (approval.value === undefined) {
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
    "daisy-btn-primary": approval.value === undefined,
    "daisy-btn-disabled": approval.value !== undefined,
    "daisy-mt-2": true,
    display: "block",
  }
})

const isApprovalButtonDisabled = computed(() => {
  return approval.value !== undefined
})
const requestNotebookApproval = async () => {
  approval.value =
    await managedApi.restNotebookCertificateApprovalController.requestApprovalForNotebook(
      props.notebook.id
    )
}
</script>

