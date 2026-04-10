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
    class="daisy-container daisy-mx-auto daisy-max-w-3xl daisy-px-4 daisy-py-6"
  >
    <div
      class="daisy-rounded-box daisy-border daisy-border-dashed daisy-border-base-300 daisy-bg-base-200/30 daisy-px-6 daisy-py-10 daisy-text-center"
    >
      <p class="daisy-m-0 daisy-text-base daisy-text-base-content/70">
        This notebook group could not be found.
      </p>
      <router-link
        :to="{ name: 'notebooks' }"
        class="daisy-btn daisy-btn-link daisy-btn-sm daisy-mt-3 daisy-inline-block"
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
