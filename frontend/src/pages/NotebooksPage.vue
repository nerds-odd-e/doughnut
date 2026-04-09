<template>
  <ContentLoader v-if="notebooks === undefined" />
  <NotebooksPageView
    v-else-if="user !== undefined"
    :catalog-items="catalogItems ?? []"
    :subscriptions="subscriptions ?? []"
    :user="user"
    @notebook-updated="handleNotebookUpdated"
    @refresh="fetchData"
  />
</template>

<script setup lang="ts">
import { inject, onMounted, ref, type Ref } from "vue"
import type {
  Notebook,
  Subscription,
  User,
} from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import {} from "@/managedApi/clientSetup"
import {
  patchNotebookInCatalogItems,
  type NotebookCatalogEntry,
} from "@/components/notebook/patchNotebookInCatalogItems"
import NotebooksPageView from "./NotebooksPageView.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"

const user = inject<Ref<User | undefined>>("currentUser")
const subscriptions = ref<Subscription[] | undefined>(undefined)
const notebooks = ref<Notebook[] | undefined>(undefined)
const catalogItems = ref<NotebookCatalogEntry[] | undefined>(undefined)

const fetchData = async () => {
  const { data: result, error } = await NotebookController.myNotebooks({})
  if (!error) {
    notebooks.value = result!.notebooks
    catalogItems.value = result!.catalogItems
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
  if (catalogItems.value) {
    catalogItems.value = patchNotebookInCatalogItems(
      catalogItems.value,
      updatedNotebook
    )
  }
}

onMounted(() => {
  fetchData()
})
</script>

