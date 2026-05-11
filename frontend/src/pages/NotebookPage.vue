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
      :fetch-notebook-page="fetchNotebook"
      :index-note-status="indexNoteStatus"
      :index-note-id="sidebarAnchorNoteId"
      @notebook-updated="handleNotebookUpdated"
      @index-note-created="fetchNotebook"
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
  NotebookRealm,
} from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebookPageReadonlySummary from "@/components/notebook/NotebookPageReadonlySummary.vue"
import NotebookPageView from "./NotebookPageView.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import {
  currentActiveNoteId,
  folderPageBreadcrumbFolders,
  folderSidebarFolderRealm,
  notebookSidebarNotebookRealm,
} from "@/composables/useCurrentNoteSidebarState"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const route = useRoute()
const storageAccessor = useStorageAccessor()
const user = inject<Ref<User | undefined>>("currentUser")
const notebookRealm = ref<NotebookRealm | undefined>(undefined)

const notebook = computed(() => notebookRealm.value?.notebook)

const isNotebookReadOnly = computed(
  () => notebookRealm.value?.readonly === true
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
    notebookRealm.value = result
    return
  }
  notebookRealm.value = undefined
}

const handleNotebookUpdated = (updatedNotebook: Notebook) => {
  const prev = notebookRealm.value
  if (prev != null) {
    notebookRealm.value = { ...prev, notebook: updatedNotebook }
  }
}

watch(
  notebookRealm,
  (c) => {
    if (!c) {
      notebookSidebarNotebookRealm.value = undefined
      return
    }
    notebookSidebarNotebookRealm.value = c
  },
  { immediate: true, deep: true }
)

watch(
  () =>
    notebookRealm.value
      ? ([
          notebookRealm.value.notebook.id,
          notebookRealm.value.indexNoteId ?? null,
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
  folderPageBreadcrumbFolders.value = []
  folderSidebarFolderRealm.value = undefined
  await fetchNotebook()
})

onBeforeUnmount(() => {
  notebookSidebarNotebookRealm.value = undefined
  folderSidebarFolderRealm.value = undefined
})
</script>
