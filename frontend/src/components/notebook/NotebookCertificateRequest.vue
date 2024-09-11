<template>
  <h4>Request to obtain certificate from assessment</h4>
  <ContentLoader v-if="loaded === false" />
  <div v-else>
    <button :class="approvalButtonClasses" :disabled="isApprovalButtonDisabled" @click="requestNotebookApproval">
      {{ approvalButtonText }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { PropType, computed, onMounted, ref } from "vue"
import { Notebook, NotebookCertificateApproval } from "@/generated/backend"
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
    btn: true,
    "btn-primary": approval.value === undefined,
    "btn-disabled": approval.value !== undefined,
    "btn-layout": true,
    "mt-2": true,
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

