<template>
  <!--
    Instead of using a daisy-drawer, we're using a responsive layout with a sidebar that
    is either a static column on md+ screens or an overlay on smaller screens.
  -->
  <div class="daisy-flex daisy-flex-col daisy-h-full">
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
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </template>
            <template v-else>
              <line x1="3" y1="6" x2="21" y2="6"></line>
              <line x1="6" y1="12" x2="21" y2="12"></line>
              <line x1="3" y1="18" x2="21" y2="18"></line>
            </template>
          </svg>
        </div>
      </button>
      <BreadcrumbWithCircle
        v-if="noteRealm"
        v-bind="{
          fromBazaar: noteRealm?.fromBazaar,
          circle: notebookForBreadcrumb?.circle,
          noteTopology: noteRealm?.note.noteTopology,
        }"
      />
    </GlobalBar>
    <!-- Overlay mask for mobile -->
    <div
      v-if="!isMdOrLarger && sidebarOpened"
      class="daisy-fixed daisy-inset-0 daisy-bg-black/50 daisy-z-30"
      @click="sidebarOpened = false"
    ></div>
    <div class="daisy-h-full daisy-relative daisy-flex daisy-flex-1 daisy-min-h-0">

    <!-- Sidebar -->
    <aside
      :class="[
        'daisy-bg-base-200 daisy-w-72 daisy-transition-all daisy-ease-in-out daisy-overflow-y-auto',
        isMdOrLarger
          ? (sidebarOpened ? 'daisy-relative' : 'daisy-hidden')
          : (sidebarOpened
              ? 'daisy-translate-x-0 daisy-fixed daisy-top-0 daisy-left-0 daisy-z-40 daisy-h-full'
              : '-daisy-translate-x-full daisy-fixed daisy-top-0 daisy-left-0 daisy-h-full')
      ]"
    >
      <NoteSidebar
        v-if="noteRealm"
        :note-realm="noteRealm"
        :notebook-id="noteRealm.notebookId"
      />
    </aside>

    <!-- Main Content -->
    <main
      class="daisy-flex-1 daisy-px-4 daisy-container daisy-mx-auto daisy-overflow-y-auto"
    >
      <div
        v-if="loadError !== null"
        class="daisy-alert daisy-alert-error daisy-my-4"
        role="alert"
      >
        {{ loadError }}
      </div>
      <ContentLoader v-else-if="resolvedNoteId === undefined" />
      <NoteShow
        v-else
        v-bind="{
          noteId: resolvedNoteId,
          expandChildren: true,
          isMinimized: isContentMinimized,
        }"
      >
        <template #note-conversation="{ noteRealm: conversationRealm }">
          <div
            v-if="Boolean(route.query.conversation)"
            class="conversation-wrapper daisy-border-t daisy-border-base-200 daisy-flex-1 daisy-flex daisy-flex-col daisy-bg-base-100/50"
          >
            <NoteConversation
              :note-id="conversationRealm.id"
              :is-maximized="isContentMinimized"
              @close-dialog="handleCloseConversation(conversationRealm)"
              @toggle-maximize="toggleMaximize"
            />
          </div>
        </template>
      </NoteShow>
    </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted, onBeforeUnmount } from "vue"

import { useRoute, useRouter } from "vue-router"
import NoteShow from "../components/notes/NoteShow.vue"
import NoteSidebar from "../components/notes/NoteSidebar.vue"
import NoteConversation from "../components/conversations/NoteConversation.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import GlobalBar from "../components/toolbars/GlobalBar.vue"
import BreadcrumbWithCircle from "../components/toolbars/BreadcrumbWithCircle.vue"
import { noteShowByNotebookSlugLocationFromNoteRealm } from "@/routes/noteShowLocation"
import type { NoteRealm, Notebook } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"

const router = useRouter()
const route = useRoute()
const storageAccessor = useStorageAccessor()

const props = defineProps({
  noteId: { type: Number, required: false },
  basename: { type: String, required: false },
  notebookId: { type: Number, required: false },
  noteSlugPath: { type: String, required: false },
})

const basenameError = ref<string | null>(null)
const basenameResolvedNoteId = ref<number | undefined>(undefined)
const notebookSlugError = ref<string | null>(null)
const notebookSlugResolvedNoteId = ref<number | undefined>(undefined)
let notebookSlugLoadGeneration = 0

const isNotebookSlugEntry = computed(
  () =>
    props.notebookId != null &&
    !Number.isNaN(props.notebookId) &&
    props.noteSlugPath !== undefined &&
    props.noteSlugPath !== ""
)

const loadError = computed(() => notebookSlugError.value ?? basenameError.value)

watch(
  () => props.noteId,
  (noteId) => {
    if (isNotebookSlugEntry.value) {
      return
    }
    if (
      noteId != null &&
      !Number.isNaN(noteId) &&
      props.basename === undefined
    ) {
      storageAccessor.value.storedApi().getNoteRealmRefAndReloadPosition(noteId)
    }
  },
  { immediate: true }
)

watch(
  () => props.basename,
  async (b) => {
    if (isNotebookSlugEntry.value) {
      return
    }
    basenameError.value = null
    basenameResolvedNoteId.value = undefined
    if (b === undefined || b === "") {
      return
    }
    try {
      const realm = await storageAccessor.value
        .storedApi()
        .loadNoteByAmbiguousBasename(b)
      basenameResolvedNoteId.value = realm.id
    } catch (e: unknown) {
      basenameError.value =
        e instanceof Error ? e.message : "Could not load note"
    }
  },
  { immediate: true }
)

watch(
  () => [props.notebookId, props.noteSlugPath] as const,
  async ([nb, path]) => {
    notebookSlugError.value = null
    const generation = ++notebookSlugLoadGeneration
    notebookSlugResolvedNoteId.value = undefined
    if (nb == null || Number.isNaN(nb) || path === undefined || path === "") {
      return
    }
    try {
      const realm = await storageAccessor.value
        .storedApi()
        .loadNoteByNotebookSlug(nb, path)
      if (generation !== notebookSlugLoadGeneration) return
      notebookSlugResolvedNoteId.value = realm.id
    } catch (e: unknown) {
      if (generation !== notebookSlugLoadGeneration) return
      notebookSlugError.value =
        e instanceof Error ? e.message : "Could not load note"
    }
  },
  { immediate: true }
)

const resolvedNoteId = computed((): number | undefined => {
  if (isNotebookSlugEntry.value) {
    if (notebookSlugResolvedNoteId.value == null) {
      return undefined
    }
    return storageAccessor.value.refOfNoteRealm(
      notebookSlugResolvedNoteId.value
    ).value?.id
  }
  if (props.noteId != null && !Number.isNaN(props.noteId)) {
    return storageAccessor.value.refOfNoteRealm(props.noteId).value?.id
  }
  if (
    props.basename !== undefined &&
    props.basename !== "" &&
    basenameResolvedNoteId.value != null
  ) {
    return storageAccessor.value.refOfNoteRealm(basenameResolvedNoteId.value)
      .value?.id
  }
  return undefined
})

const noteRealm = computed(() => {
  const id = resolvedNoteId.value
  if (id == null) return undefined
  return storageAccessor.value.refOfNoteRealm(id).value
})

const routeNotebookId = computed((): number | undefined => {
  if (
    props.notebookId == null ||
    Number.isNaN(props.notebookId) ||
    props.noteSlugPath === undefined ||
    props.noteSlugPath === ""
  ) {
    return undefined
  }
  return props.notebookId
})

const notebookForBreadcrumb = ref<Notebook | undefined>(undefined)

watch(
  () => routeNotebookId.value ?? noteRealm.value?.notebookId,
  async (notebookId) => {
    if (notebookId == null || Number.isNaN(Number(notebookId))) {
      notebookForBreadcrumb.value = undefined
      return
    }
    if (notebookForBreadcrumb.value?.id === notebookId) {
      return
    }
    const { data, error } = await NotebookController.get({
      path: { notebook: notebookId },
    })
    if (!error && data) {
      notebookForBreadcrumb.value = data
    }
  },
  { immediate: true }
)

const sidebarOpened = ref(false)
const isContentMinimized = ref(false)

const toggleMaximize = () => {
  isContentMinimized.value = !isContentMinimized.value
}

const handleCloseConversation = (conversationRealm: NoteRealm) => {
  isContentMinimized.value = false
  router.replace({
    ...noteShowByNotebookSlugLocationFromNoteRealm(conversationRealm),
    query: {},
  })
}

// Track window width so we can decide when to show sidebar by default
const windowWidth = ref(window.innerWidth)

function handleResize() {
  windowWidth.value = window.innerWidth
}

onMounted(() => {
  window.addEventListener("resize", handleResize)
  // Open sidebar if width >= md (~768px)
  if (windowWidth.value >= 768) {
    sidebarOpened.value = true
  }
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
})

const isMdOrLarger = computed(() => windowWidth.value >= 768)

// Close sidebar automatically when the resolved note changes, to maintain a fresh state for each note
watch(
  () => resolvedNoteId.value,
  () => {
    if (!isMdOrLarger.value) {
      sidebarOpened.value = false
    }
  }
)
</script>

<style scoped>
/* Ensure the root container takes full height */
.note-show-page {
  height: 100%;
  display: flex;
  overflow: hidden;
}

/* Set max height for both sidebar and main content to enable independent scrolling */
aside {
  max-height: 100%;
}

main {
  max-height: 100%;
}

/* Ensure the conversation wrapper takes remaining height */
.conversation-wrapper {
  max-height: 100%;
  overflow: hidden;
}

/* Extra convenience to override base daisyUI for transitions. */
</style>
