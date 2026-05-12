<template>
  <div class="daisy-flex daisy-flex-col daisy-h-full">
    <GlobalBar>
      <button
        type="button"
        class="daisy-btn daisy-btn-sm daisy-btn-ghost"
        :aria-label="sidebarOpened ? 'Hide sidebar' : 'Show sidebar'"
        :title="sidebarOpened ? 'Hide sidebar' : 'Show sidebar'"
        @click="sidebarOpened = !sidebarOpened"
      >
        <PanelLeftClose
          v-if="sidebarOpened"
          class="daisy-w-6 daisy-h-6"
          aria-hidden="true"
        />
        <PanelLeft
          v-else
          class="daisy-w-6 daisy-h-6"
          aria-hidden="true"
        />
      </button>
      <BreadcrumbWithCircle
        v-if="currentNotebookRealm"
        :notebook-realm="currentNotebookRealm"
        :ancestor-folders="breadcrumbFolders"
      />
    </GlobalBar>
    <div
      v-if="!isMdOrLarger && sidebarOpened"
      class="notebook-sidebar-drawer-backdrop daisy-fixed daisy-inset-x-0 daisy-bottom-0 daisy-bg-black/50 daisy-z-30"
      @click="sidebarOpened = false"
    />
    <div
      class="daisy-h-full daisy-relative daisy-flex daisy-flex-1 daisy-min-h-0"
    >
      <aside :class="sidebarClasses">
        <Sidebar
          v-if="currentNotebookId != null"
          :key="currentNotebookId"
          :active-note-realm="activeNoteRealm"
          :notebook-id="currentNotebookId"
          :notebook-readonly="sidebarNotebookRealm?.readonly === true"
          :active-folder-realm="activeFolderRealm"
          :breadcrumb-folders="breadcrumbFolders"
        />
      </aside>
      <main
        class="daisy-flex-1 daisy-px-4 daisy-container daisy-mx-auto daisy-overflow-y-auto"
      >
        <slot>
          <RouterView v-slot="{ Component }">
            <component :is="Component" v-bind="routeViewProps" />
          </RouterView>
        </slot>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"
import { RouterView, useRoute } from "vue-router"
import type {
  Folder,
  FolderRealm,
  NoteRealm,
  NotebookRealm,
} from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { PanelLeft, PanelLeftClose } from "lucide-vue-next"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import Sidebar from "@/components/notes/Sidebar.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const route = useRoute()
const storageAccessor = useStorageAccessor()
const SIDEBAR_BREAKPOINT_PX = 768

const sidebarOpened = ref(false)
const windowWidth = ref(
  typeof window !== "undefined" ? window.innerWidth : 1024
)

const isMdOrLarger = computed(() => windowWidth.value >= SIDEBAR_BREAKPOINT_PX)

// Only populated on notebookPage route
const activeNotebookRealm = ref<NotebookRealm | undefined>(undefined)
const activeFolderRealm = ref<FolderRealm | undefined>(undefined)

async function fetchNotebookPage() {
  const notebookId = Number(route.params.notebookId)
  const { data, error } = await NotebookController.get({
    path: { notebook: notebookId },
  })
  activeNotebookRealm.value = !error && data ? data : undefined
}

const activeNoteRealm = computed((): NoteRealm | undefined => {
  if (route.name !== "noteShow") return undefined
  const id = Number(route.params.noteId)
  if (!Number.isFinite(id)) return undefined
  return storageAccessor.value.refOfNoteRealm(id).value
})

const breadcrumbFolders = computed(
  (): Folder[] =>
    activeNoteRealm.value?.ancestorFolders ??
    activeFolderRealm.value?.ancestorFolders ??
    []
)

const sidebarNotebookRealm = computed(
  (): NotebookRealm | undefined =>
    activeNotebookRealm.value ?? activeFolderRealm.value?.notebookRealm
)

const currentNotebookRealm = computed(
  (): NotebookRealm | undefined =>
    activeNoteRealm.value?.notebookRealm ?? sidebarNotebookRealm.value
)

const currentNotebookId = computed(
  () => currentNotebookRealm.value?.notebook?.id
)

const desktopSidebarClass = computed(() =>
  sidebarOpened.value ? "daisy-relative" : "daisy-hidden"
)

const mobileSidebarClass = computed(() => [
  "notebook-sidebar-drawer daisy-fixed daisy-left-0 daisy-z-40",
  sidebarOpened.value ? "daisy-translate-x-0" : "-daisy-translate-x-full",
])

const sidebarClasses = computed(() => [
  "daisy-bg-base-200 daisy-w-72 daisy-transition-all daisy-ease-in-out daisy-flex daisy-flex-col daisy-overflow-x-visible",
  ...(isMdOrLarger.value
    ? [desktopSidebarClass.value]
    : mobileSidebarClass.value),
])

const routeViewProps = computed(() => {
  if (route.name === "notebookPage") {
    return { notebookRealm: activeNotebookRealm.value, fetchNotebookPage }
  }
  if (route.name === "folderPage") {
    return { folderRealm: activeFolderRealm.value, fetchFolderPage }
  }
  return {}
})

async function fetchFolderPage() {
  const notebookId = Number(route.params.notebookId)
  const folderId = Number(route.params.folderId)
  const { data: page, error } = await NotebookController.getFolderPage({
    path: { notebook: notebookId, folder: folderId },
  })
  if (!error && page?.notebookRealm?.notebook) {
    activeFolderRealm.value = page
    return
  }
  activeFolderRealm.value = undefined
}

watch(
  () => ({
    isNotebookPage: route.name === "notebookPage",
    notebookId: route.params.notebookId,
  }),
  async ({ isNotebookPage }) => {
    if (!isNotebookPage) {
      activeNotebookRealm.value = undefined
      return
    }
    await fetchNotebookPage()
  },
  { immediate: true }
)

watch(
  () => ({
    isFolderPage: route.name === "folderPage",
    notebookId: route.params.notebookId,
    folderId: route.params.folderId,
  }),
  async ({ isFolderPage }) => {
    if (!isFolderPage) {
      activeFolderRealm.value = undefined
      return
    }
    await fetchFolderPage()
  },
  { immediate: true }
)

const handleResize = () => {
  windowWidth.value = window.innerWidth
}

const closeSidebarOnMobile = () => {
  if (!isMdOrLarger.value) {
    sidebarOpened.value = false
  }
}

watch(
  () => [
    currentNotebookId.value,
    route.name === "noteShow" ? route.params.noteId : undefined,
  ],
  closeSidebarOnMobile
)

onMounted(() => {
  window.addEventListener("resize", handleResize)
  if (windowWidth.value >= SIDEBAR_BREAKPOINT_PX) {
    sidebarOpened.value = true
  }
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
})
</script>

<style scoped lang="scss">
@use "@/assets/menu-variables.scss" as *;

.notebook-sidebar-drawer {
  @media (max-width: theme("screens.md")) {
    top: calc(#{$main-menu-height-mobile} + env(safe-area-inset-top, 0px));
    bottom: 0;
    height: auto;
  }
}

.notebook-sidebar-drawer-backdrop {
  @media (max-width: theme("screens.md")) {
    top: calc(#{$main-menu-height-mobile} + env(safe-area-inset-top, 0px));
  }
}

aside,
main {
  max-height: 100%;
}
</style>
