<template>
  <NotebookGroupPageView
    v-if="user !== undefined"
    :group="resolvedGroup"
    :subscriptions="subscriptions ?? []"
    :user="user"
    @notebook-updated="handleNotebookUpdated"
    @refresh="fetchData"
  />
</template>

<script setup lang="ts">
import { computed, inject, onMounted, type Ref } from "vue"
import type {
  NotebookCatalogGroupItem,
  User,
} from "@generated/donut-backend-api"
import { useNotebookCatalog } from "@/composables/useNotebookCatalog"
import NotebookGroupPageView from "./NotebookGroupPageView.vue"

const props = defineProps<{
  groupId: number
}>()

const user = inject<Ref<User | undefined>>("currentUser")
const { catalogItems, subscriptions, fetchData, handleNotebookUpdated } =
  useNotebookCatalog()

const resolvedGroup = computed(
  (): NotebookCatalogGroupItem | null | undefined => {
    const items = catalogItems.value
    if (!items) {
      return undefined
    }
    return (
      items.find(
        (i): i is NotebookCatalogGroupItem =>
          i.type === "notebookGroup" && i.id === props.groupId
      ) ?? null
    )
  }
)

onMounted(() => {
  fetchData()
})
</script>
