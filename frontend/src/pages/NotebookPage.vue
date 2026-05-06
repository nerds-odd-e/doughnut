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
      :show-add-first-note="indexNoteStatus === 'absent'"
      :index-note-status="indexNoteStatus"
      :index-note-id="sidebarAnchorNoteId"
      @notebook-updated="handleNotebookUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import {
  inject,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
  computed,
  type Ref,
} from "vue"
import { useRoute } from "vue-router"
import type {
  Notebook,
  User,
  NotebookPageClientView,
} from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebookPageReadonlySummary from "@/components/notebook/NotebookPageReadonlySummary.vue"
import NotebookPageView from "./NotebookPageView.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import {
  currentActiveNoteId,
  currentNotebookId,
  notebookSidebarNotebookClientView,
} from "@/composables/useCurrentNoteSidebarState"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const route = useRoute()
const storageAccessor = useStorageAccessor()
const user = inject<Ref<User | undefined>>("currentUser")
const notebookClient = ref<NotebookPageClientView | undefined>(undefined)

const notebook = computed(() => notebookClient.value?.notebook)

const isNotebookReadOnly = computed(
  () => notebookClient.value?.readonly === true
)

const sidebarAnchorNoteId = ref<number | undefined>()
const indexNoteStatus = ref<"pending" | "present" | "absent">("pending")
let indexResolveGeneration = 0

const fetchNotebook = async () => {
  const notebookId = Number(route.params.notebookId)
  const { data: result, error } = await NotebookController.get({
    path: { notebook: notebookId },
  })
  if (!error && result) {
    notebookClient.value = result
    return
  }
  notebookClient.value = undefined
}

const handleNotebookUpdated = (updatedNotebook: Notebook) => {
  const prev = notebookClient.value
  if (prev != null) {
    notebookClient.value = { ...prev, notebook: updatedNotebook }
  }
}

watch(
  notebookClient,
  (c) => {
    if (!c) {
      notebookSidebarNotebookClientView.value = undefined
      currentNotebookId.value = undefined
      return
    }
    currentNotebookId.value = c.notebook.id
    notebookSidebarNotebookClientView.value = c
  },
  { immediate: true, deep: true }
)

watch(
  () =>
    notebookClient.value
      ? ([
          notebookClient.value.notebook.id,
          notebookClient.value.indexNoteId ?? null,
        ] as const)
      : undefined,
  async (key) => {
    if (key === undefined) {
      sidebarAnchorNoteId.value = undefined
      indexNoteStatus.value = "pending"
      currentActiveNoteId.value = undefined
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

watch(
  sidebarAnchorNoteId,
  (id) => {
    currentActiveNoteId.value = id
  },
  { immediate: true }
)

watch(
  () => route.params.notebookId,
  async () => {
    await fetchNotebook()
  }
)

onMounted(async () => {
  await fetchNotebook()
})

onBeforeUnmount(() => {
  notebookSidebarNotebookClientView.value = undefined
})
</script>
