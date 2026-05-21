<template>
  <ContentLoader v-if="catalogItems === undefined" />
  <NotebookGroupPageView
    v-else-if="user !== undefined && resolvedGroup !== undefined"
    :group="resolvedGroup"
    :subscriptions="subscriptions ?? []"
    :user="user"
    @notebook-updated="handleNotebookUpdated"
    @refresh="fetchData"
  />
  <main
    v-else-if="user !== undefined"
    class="container mx-auto max-w-3xl px-4 py-6"
  >
    <div
      class="rounded-box border border-dashed border-base-300 bg-base-200/30 px-6 py-10 text-center"
    >
      <p class="m-0 text-base text-base-content/70">
        This notebook group could not be found.
      </p>
      <router-link
        :to="{ name: 'notebooks' }"
        class="daisy-btn daisy-btn-link daisy-btn-sm mt-3 inline-block"
      >
        Back to My notebooks
      </router-link>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, type Ref } from "vue"
import type {
  NotebookCatalogGroupItem,
  User,
} from "@generated/doughnut-backend-api"
import { useMyNotebooksCatalog } from "@/composables/useMyNotebooksCatalog"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import NotebookGroupPageView from "./NotebookGroupPageView.vue"

const props = defineProps<{
  groupId: number
}>()

const user = inject<Ref<User | undefined>>("currentUser")
const { catalogItems, subscriptions, fetchData, handleNotebookUpdated } =
  useMyNotebooksCatalog()

const resolvedGroup = computed((): NotebookCatalogGroupItem | undefined => {
  const items = catalogItems.value
  if (!items || Number.isNaN(props.groupId)) {
    return undefined
  }
  const found = items.find(
    (i): i is NotebookCatalogGroupItem =>
      i.type === "notebookGroup" && i.id === props.groupId
  )
  return found
})

onMounted(() => {
  fetchData()
})
</script>
