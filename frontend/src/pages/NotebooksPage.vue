<template>
  <ContainerPage v-bind="{ contentLoaded: notebooks !== undefined }">
    <NotebooksPageView
      v-if="notebooks !== undefined && user !== undefined"
      :notebooks="notebooks"
      :subscriptions="subscriptions ?? []"
      :user="user"
      @notebook-updated="handleNotebookUpdated"
      @refresh="fetchData"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import { inject, onMounted, ref, type Ref } from "vue"
import type { Notebook, Subscription, User } from "@generated/backend"
import { NotebookController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import NotebooksPageView from "./NotebooksPageView.vue"
import ContainerPage from "./commons/ContainerPage.vue"

const user = inject<Ref<User | undefined>>("currentUser")
const subscriptions = ref<Subscription[] | undefined>(undefined)
const notebooks = ref<Notebook[] | undefined>(undefined)

const fetchData = async () => {
  const { data: result, error } = await NotebookController.myNotebooks({})
  if (!error) {
    notebooks.value = result!.notebooks
    subscriptions.value = result!.subscriptions
  }
}

const handleNotebookUpdated = (updatedNotebook: Notebook) => {
  if (notebooks.value) {
    const index = notebooks.value.findIndex((n) => n.id === updatedNotebook.id)
    if (index !== -1) {
      notebooks.value[index] = updatedNotebook
    }
  }
}

onMounted(() => {
  fetchData()
})
</script>

