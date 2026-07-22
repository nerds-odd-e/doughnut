<template>
  <ContentLoader v-if="notebook === undefined" />
  <div v-else class="pt-0 pb-4">
    <NotebookPageReadonlySummary
      v-if="isNotebookReadOnly"
      :notebook="notebook"
    />
    <NotebookPageView
      v-else
      :notebook="notebook"
      :user="user"
      :fetch-notebook-page="fetchNotebookPage"
      :index-content="props.notebookRealm?.indexContent ?? null"
      @notebook-updated="() => fetchNotebookPage()"
      @index-content-updated="fetchNotebookPage"
    />
  </div>
</template>

<script setup lang="ts">
import { inject, computed, type Ref } from "vue"
import type { User, NotebookRealm } from "@generated/doughnut-backend-api"
import NotebookPageReadonlySummary from "@/components/notebook/NotebookPageReadonlySummary.vue"
import NotebookPageView from "./NotebookPageView.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"

const props = defineProps<{
  notebookRealm: NotebookRealm | undefined
  fetchNotebookPage: () => Promise<void>
}>()

const user = inject<Ref<User | undefined>>("currentUser")

const notebook = computed(() => props.notebookRealm?.notebook)

const isNotebookReadOnly = computed(
  () => props.notebookRealm?.readonly === true
)
</script>
