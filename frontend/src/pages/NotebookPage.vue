<template>
  <ContentLoader v-if="notebook === undefined" />
  <NotebookPageView
    v-else-if="user !== undefined"
    :notebook="notebook"
    :user="user"
    :approval="approval"
    :approval-loaded="approvalLoaded"
    :additional-instructions="aiAssistant?.additionalInstructionsToAi || ''"
    @notebook-updated="handleNotebookUpdated"
  />
</template>

<script setup lang="ts">
import { inject, onMounted, ref, type Ref } from "vue"
import { useRoute } from "vue-router"
import type {
  Notebook,
  User,
  NotebookCertificateApproval,
  NotebookAiAssistant,
} from "@generated/backend"
import {
  NotebookController,
  NotebookCertificateApprovalController,
} from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import NotebookPageView from "./NotebookPageView.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"

const route = useRoute()
const user = inject<Ref<User | undefined>>("currentUser")
const notebook = ref<Notebook | undefined>(undefined)
const approval = ref<NotebookCertificateApproval | undefined>(undefined)
const approvalLoaded = ref(false)
const aiAssistant = ref<NotebookAiAssistant | undefined>(undefined)

const fetchNotebook = async () => {
  const notebookId = Number(route.params.notebookId)
  const { data: result, error } = await NotebookController.get({
    path: { notebook: notebookId },
  })
  if (!error) {
    notebook.value = result!
  }
}

const fetchApproval = async () => {
  if (!notebook.value) return
  const { data: dto, error } = await apiCallWithLoading(() =>
    NotebookCertificateApprovalController.getApprovalForNotebook({
      path: { notebook: notebook.value!.id },
    })
  )
  if (!error) {
    approval.value = dto!.approval
    approvalLoaded.value = true
  } else {
    approvalLoaded.value = true
  }
}

const fetchAiAssistant = async () => {
  if (!notebook.value) return
  const { data: assistant, error } = await apiCallWithLoading(() =>
    NotebookController.getAiAssistant({
      path: { notebook: notebook.value!.id },
    })
  )
  if (!error) {
    aiAssistant.value = assistant!
  }
}

const handleNotebookUpdated = (updatedNotebook: Notebook) => {
  notebook.value = updatedNotebook
}

onMounted(async () => {
  await fetchNotebook()
  if (notebook.value) {
    // Fetch approval and AI assistant in parallel, but don't block rendering
    fetchApproval()
    fetchAiAssistant()
  }
})
</script>

