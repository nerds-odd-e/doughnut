<template>
  <NotebookSidebarLayout>
    <NotebookPage
      :notebook-realm="notebookRealm"
      :fetch-notebook-page="fetchNotebookPage"
    />
  </NotebookSidebarLayout>
</template>

<script setup lang="ts">
import { ref, watch } from "vue"
import { useRoute } from "vue-router"
import type { NotebookRealm } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebookSidebarLayout from "@/layouts/NotebookSidebarLayout.vue"
import NotebookPage from "@/pages/NotebookPage.vue"

const route = useRoute()
const notebookRealm = ref<NotebookRealm | undefined>(undefined)

const fetchNotebookPage = async () => {
  const notebookId = Number(route.params.notebookId)
  if (!Number.isFinite(notebookId)) {
    notebookRealm.value = undefined
    return
  }
  const { data, error } = await NotebookController.get({
    path: { notebook: notebookId },
  })
  notebookRealm.value = !error && data ? data : undefined
}

watch(
  () => ({
    name: route.name,
    notebookId: route.params.notebookId,
  }),
  async ({ name }) => {
    if (name !== "notebookPage") {
      notebookRealm.value = undefined
      return
    }
    await fetchNotebookPage()
  },
  { immediate: true }
)
</script>
