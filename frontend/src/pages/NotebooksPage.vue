<template>
  <ContentLoader v-if="catalogItems === undefined" />
  <NotebooksPageView
    v-else-if="user !== undefined"
    :catalog-items="catalogItems"
    :subscriptions="subscriptions ?? []"
    :user="user"
    @notebook-updated="handleNotebookUpdated"
    @refresh="fetchData"
  />
</template>

<script setup lang="ts">
import { inject, onMounted, type Ref } from "vue"
import type { User } from "@generated/doughnut-backend-api"
import { useMyNotebooksCatalog } from "@/composables/useMyNotebooksCatalog"
import NotebooksPageView from "./NotebooksPageView.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"

const user = inject<Ref<User | undefined>>("currentUser")
const { catalogItems, subscriptions, fetchData, handleNotebookUpdated } =
  useMyNotebooksCatalog()

onMounted(() => {
  fetchData()
})
</script>

