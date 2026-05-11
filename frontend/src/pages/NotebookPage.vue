<template>
  <ContentLoader v-if="notebook === undefined" />
  <div v-else class="daisy-py-4">
    <NotebookPageReadonlySummary
      v-if="isNotebookReadOnly"
      :notebook="notebook"
    />
    <NotebookPageView
      v-else
      :notebook="notebook"
      :user="user"
      :fetch-notebook-page="fetchNotebookPage"
      :index-note-status="indexNoteStatus"
      :index-note-id="sidebarAnchorNoteId"
      @notebook-updated="() => fetchNotebookPage()"
      @index-note-created="fetchNotebookPage"
    />
  </div>
</template>

<script setup lang="ts">
import { inject, ref, watch, computed, type Ref } from "vue"
import type { User, NotebookRealm } from "@generated/doughnut-backend-api"
import NotebookPageReadonlySummary from "@/components/notebook/NotebookPageReadonlySummary.vue"
import NotebookPageView from "./NotebookPageView.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const props = defineProps<{
  notebookRealm: NotebookRealm | undefined
  fetchNotebookPage: () => Promise<void>
}>()

const storageAccessor = useStorageAccessor()
const user = inject<Ref<User | undefined>>("currentUser")

const notebook = computed(() => props.notebookRealm?.notebook)

const isNotebookReadOnly = computed(
  () => props.notebookRealm?.readonly === true
)

const sidebarAnchorNoteId = ref<number | undefined>()
const indexNoteStatus = ref<"pending" | "present" | "absent">("pending")
let indexResolveGeneration = 0

watch(
  () =>
    props.notebookRealm
      ? ([
          props.notebookRealm.notebook.id,
          props.notebookRealm.indexNoteId ?? null,
        ] as const)
      : undefined,
  async (key) => {
    if (key === undefined) {
      sidebarAnchorNoteId.value = undefined
      indexNoteStatus.value = "pending"
      return
    }

    const [, indexNoteId] = key
    const gen = ++indexResolveGeneration
    sidebarAnchorNoteId.value = undefined
    indexNoteStatus.value = "pending"

    if (indexNoteId == null) {
      indexNoteStatus.value = "absent"
      return
    }

    try {
      await storageAccessor.value.storedApi().loadNoteRealm(indexNoteId)
      if (gen !== indexResolveGeneration) return
      sidebarAnchorNoteId.value = indexNoteId
      indexNoteStatus.value = "present"
    } catch {
      if (gen !== indexResolveGeneration) return
      indexNoteStatus.value = "absent"
    }
  },
  { immediate: true }
)
</script>
