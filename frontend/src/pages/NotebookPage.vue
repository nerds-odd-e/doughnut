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
      :show-add-first-note="indexSlugStatus === 'absent'"
      :index-slug-status="indexSlugStatus"
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
  NotebookClientView,
} from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebookPageReadonlySummary from "@/components/notebook/NotebookPageReadonlySummary.vue"
import NotebookPageView from "./NotebookPageView.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import {
  currentActiveNoteId,
  currentNotebookId,
  notebookSidebarNotebookPageContext,
} from "@/composables/useCurrentNoteSidebarState"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const route = useRoute()
const storageAccessor = useStorageAccessor()
const user = inject<Ref<User | undefined>>("currentUser")
const notebookClient = ref<NotebookClientView | undefined>(undefined)

const notebook = computed(() => notebookClient.value?.notebook)

const isNotebookReadOnly = computed(
  () => notebookClient.value?.readonly === true
)

const sidebarAnchorNoteId = ref<number | undefined>()
/** Resolves optional root `index` slug; separate from `sidebarAnchorNoteId` during async load. */
const indexSlugStatus = ref<"pending" | "present" | "absent">("pending")
let indexSlugResolveGeneration = 0

const resolveSidebarRealm = async (nb: Notebook) => {
  const gen = ++indexSlugResolveGeneration
  sidebarAnchorNoteId.value = undefined
  indexSlugStatus.value = "pending"
  try {
    const realm = await storageAccessor.value
      .storedApi()
      .loadNoteByNotebookSlug(nb.id, "index")
    if (gen !== indexSlugResolveGeneration) return
    sidebarAnchorNoteId.value = realm.id
    indexSlugStatus.value = "present"
  } catch {
    if (gen !== indexSlugResolveGeneration) return
    indexSlugStatus.value = "absent"
  }
}

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
  [notebook, isNotebookReadOnly],
  ([nb, ro]) => {
    if (nb) {
      currentNotebookId.value = nb.id
      notebookSidebarNotebookPageContext.value = {
        notebook: nb,
        isNotebookReadOnly: ro,
      }
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

watch(notebook, async (nb) => {
  if (nb) {
    await resolveSidebarRealm(nb)
  } else {
    sidebarAnchorNoteId.value = undefined
    indexSlugStatus.value = "pending"
    currentActiveNoteId.value = undefined
    currentNotebookId.value = undefined
    notebookSidebarNotebookPageContext.value = undefined
  }
})

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
  notebookSidebarNotebookPageContext.value = undefined
})
</script>
