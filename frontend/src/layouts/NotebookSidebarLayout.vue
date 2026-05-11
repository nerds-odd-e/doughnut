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
        v-if="sidebarRealm"
        v-bind="{
          notebookView: sidebarRealm.notebookView,
          ancestorFolders: sidebarRealm.ancestorFolders ?? [],
        }"
      />
      <BreadcrumbWithCircle
        v-else-if="sidebarNotebookRealm"
        :notebook-view="sidebarNotebookRealm"
        :ancestor-folders="chromeBreadcrumbAncestorFolders"
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
      <aside
        :class="[
          'daisy-bg-base-200 daisy-w-72 daisy-transition-all daisy-ease-in-out daisy-flex daisy-flex-col daisy-overflow-x-visible',
          !isMdOrLarger && 'notebook-sidebar-drawer',
          isMdOrLarger
            ? sidebarOpened
              ? 'daisy-relative'
              : 'daisy-hidden'
            : sidebarOpened
              ? 'daisy-translate-x-0 daisy-fixed daisy-left-0 daisy-z-40'
              : '-daisy-translate-x-full daisy-fixed daisy-left-0 daisy-z-40',
        ]"
      >
        <Sidebar
          v-if="sidebarNotebookId != null && !Number.isNaN(sidebarNotebookId)"
          :active-note-realm="sidebarRealm"
          :notebook-id="sidebarNotebookId"
          :notebook-realm="sidebarNotebookRealm"
          :active-folder="activeFolderForSidebar()"
        />
      </aside>
      <main
        class="daisy-flex-1 daisy-px-4 daisy-container daisy-mx-auto daisy-overflow-y-auto"
      >
        <slot>
          <RouterView v-slot="{ Component }">
            <component
              :is="Component"
              v-bind="
                route.name === 'folderPage'
                  ? { folderRealm, fetchFolderPage }
                  : {}
              "
            />
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
} from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { PanelLeft, PanelLeftClose } from "lucide-vue-next"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import Sidebar from "@/components/notes/Sidebar.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import {
  folderPageBreadcrumbFolders,
  folderSidebarFolderRealm,
  notebookSidebarNotebookRealm,
  resetNotebookSidebarState,
} from "@/composables/useCurrentNoteSidebarState"
import { folderBreadcrumbChainFromFlatIndex } from "@/utils/folderBreadcrumbChain"

const route = useRoute()
const storageAccessor = useStorageAccessor()

const activeFolderRef = ref<FolderRealm | null>(null)
function activeFolderForSidebar() {
  return activeFolderRef
}

const sidebarOpened = ref(false)
const windowWidth = ref(
  typeof window !== "undefined" ? window.innerWidth : 1024
)

const isMdOrLarger = computed(() => windowWidth.value >= 768)

const sidebarNotebookRealm = computed(() => notebookSidebarNotebookRealm.value)

const sidebarNotebookId = computed(
  () => notebookSidebarNotebookRealm.value?.notebook.id
)

const sidebarRealm = computed((): NoteRealm | undefined => {
  if (route.name !== "noteShow") return undefined
  const id = Number(route.params.noteId)
  if (!Number.isFinite(id)) return undefined
  return storageAccessor.value.refOfNoteRealm(id).value
})

const chromeBreadcrumbAncestorFolders = computed((): Folder[] => {
  if (route.name === "folderPage") {
    return folderPageBreadcrumbFolders.value
  }
  return []
})

const folderRealm = ref<FolderRealm | undefined>(undefined)

const fetchFolderPage = async () => {
  const notebookId = Number(route.params.notebookId)
  const folderId = Number(route.params.folderId)
  const { data: page, error } = await NotebookController.getFolderPage({
    path: { notebook: notebookId, folder: folderId },
  })
  if (!error && page?.notebookView?.notebook) {
    folderRealm.value = page
    const { data: indexRows, error: indexErr } =
      await NotebookController.listNotebookFolderIndex({
        path: { notebook: notebookId },
      })
    if (!indexErr && indexRows) {
      folderPageBreadcrumbFolders.value = folderBreadcrumbChainFromFlatIndex(
        page.folder,
        indexRows
      )
    } else {
      folderPageBreadcrumbFolders.value = [page.folder]
    }
    activeFolderRef.value = page
    return
  }
  folderRealm.value = undefined
  folderPageBreadcrumbFolders.value = []
}

watch(
  folderRealm,
  (c) => {
    folderSidebarFolderRealm.value = c
    const chromeNotebookId = c?.notebookView?.notebook?.id
    if (chromeNotebookId == null) {
      notebookSidebarNotebookRealm.value = undefined
      return
    }
    notebookSidebarNotebookRealm.value = { ...(c as FolderRealm).notebookView }
  },
  { immediate: true, deep: true }
)

watch(
  () => ({
    isFolderPage: route.name === "folderPage",
    notebookId: route.params.notebookId,
    folderId: route.params.folderId,
  }),
  async ({ isFolderPage }) => {
    if (!isFolderPage) {
      folderRealm.value = undefined
      folderSidebarFolderRealm.value = undefined
      folderPageBreadcrumbFolders.value = []
      activeFolderRef.value = null
      return
    }
    await fetchFolderPage()
  },
  { immediate: true }
)

const handleResize = () => {
  windowWidth.value = window.innerWidth
}

watch(
  () => notebookSidebarNotebookRealm.value?.notebook.id,
  () => {
    if (!isMdOrLarger.value) {
      sidebarOpened.value = false
    }
  }
)

watch(
  () => (route.name === "noteShow" ? Number(route.params.noteId) : undefined),
  () => {
    if (!isMdOrLarger.value) {
      sidebarOpened.value = false
    }
  }
)

onMounted(() => {
  window.addEventListener("resize", handleResize)
  if (windowWidth.value >= 768) {
    sidebarOpened.value = true
  }
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
  resetNotebookSidebarState()
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

aside {
  max-height: 100%;
}

main {
  max-height: 100%;
}
</style>
