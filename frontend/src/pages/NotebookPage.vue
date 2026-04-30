<template>
  <ContentLoader v-if="notebook === undefined" />
  <div v-else class="daisy-flex daisy-flex-col daisy-h-full">
    <GlobalBar>
      <button
        role="button"
        class="daisy-btn daisy-btn-sm daisy-btn-ghost"
        :class="{ 'sidebar-expanded': sidebarOpened }"
        title="toggle sidebar"
        @click="sidebarOpened = !sidebarOpened"
      >
        <div class="daisy-w-4 daisy-h-4">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="w-4 h-4"
          >
            <template v-if="sidebarOpened">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </template>
            <template v-else>
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="6" y1="12" x2="21" y2="12" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </template>
          </svg>
        </div>
      </button>
      <div
        class="daisy-text-sm daisy-breadcrumbs daisy-max-w-full daisy-min-w-0"
      >
        <ul class="daisy-m-0 daisy-pl-0">
          <li v-if="isNotebookReadOnly">
            <router-link :to="{ name: 'bazaar' }">Bazaar</router-link>
          </li>
          <template v-else>
            <li>
              <router-link :to="{ name: 'notebooks' }">Notebooks</router-link>
            </li>
            <li v-if="notebook.circle">
              <router-link
                :to="{
                  name: 'circleShow',
                  params: { circleId: notebook.circle.id },
                }"
                >{{ notebook.circle.name }}</router-link
              >
            </li>
          </template>
          <li>{{ notebook.name }}</li>
        </ul>
      </div>
    </GlobalBar>
    <div
      v-if="!isMdOrLarger && sidebarOpened"
      class="daisy-fixed daisy-inset-0 daisy-bg-black/50 daisy-z-30"
      @click="sidebarOpened = false"
    />
    <div
      class="daisy-h-full daisy-relative daisy-flex daisy-flex-1 daisy-min-h-0"
    >
      <aside
        :class="[
          'daisy-bg-base-200 daisy-w-72 daisy-transition-all daisy-ease-in-out daisy-overflow-y-auto',
          isMdOrLarger
            ? sidebarOpened
              ? 'daisy-relative'
              : 'daisy-hidden'
            : sidebarOpened
              ? 'daisy-translate-x-0 daisy-fixed daisy-top-0 daisy-left-0 daisy-z-40 daisy-h-full'
              : '-daisy-translate-x-full daisy-fixed daisy-top-0 daisy-left-0 daisy-h-full',
        ]"
      >
        <NoteSidebar
          v-if="notebook"
          :note-realm="sidebarRealm"
          :notebook-id="notebook.id"
        />
      </aside>
      <main
        class="daisy-flex-1 daisy-px-4 daisy-container daisy-mx-auto daisy-overflow-y-auto"
      >
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
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  inject,
  onMounted,
  onBeforeUnmount,
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
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import NoteSidebar from "@/components/notes/NoteSidebar.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const route = useRoute()
const storageAccessor = useStorageAccessor()
const user = inject<Ref<User | undefined>>("currentUser")
const notebookClient = ref<NotebookClientView | undefined>(undefined)

const notebook = computed(() => notebookClient.value?.notebook)

const isNotebookReadOnly = computed(
  () => notebookClient.value?.readonly === true
)

const sidebarOpened = ref(false)
const sidebarAnchorNoteId = ref<number | undefined>()
/** Resolves optional root `index` slug; separate from `sidebarAnchorNoteId` during async load. */
const indexSlugStatus = ref<"pending" | "present" | "absent">("pending")
let indexSlugResolveGeneration = 0
const windowWidth = ref(
  typeof window !== "undefined" ? window.innerWidth : 1024
)

const sidebarRealm = computed(() => {
  const id = sidebarAnchorNoteId.value
  if (id == null) return undefined
  return storageAccessor.value.refOfNoteRealm(id).value
})

const isMdOrLarger = computed(() => windowWidth.value >= 768)

const handleResize = () => {
  windowWidth.value = window.innerWidth
}

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

watch(notebook, async (nb) => {
  if (nb) {
    await resolveSidebarRealm(nb)
  } else {
    sidebarAnchorNoteId.value = undefined
    indexSlugStatus.value = "pending"
  }
})

watch(
  () => notebook.value?.id,
  () => {
    if (!isMdOrLarger.value) {
      sidebarOpened.value = false
    }
  }
)

watch(
  () => route.params.notebookId,
  async () => {
    await fetchNotebook()
  }
)

onMounted(async () => {
  window.addEventListener("resize", handleResize)
  if (windowWidth.value >= 768) {
    sidebarOpened.value = true
  }
  await fetchNotebook()
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
})
</script>

<style scoped>
aside {
  max-height: 100%;
}

main {
  max-height: 100%;
}
</style>
