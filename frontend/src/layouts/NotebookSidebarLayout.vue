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
        v-if="noteRealmForBreadcrumb"
        v-bind="{
          notebookView: noteRealmForBreadcrumb.notebookView,
          ancestorFolders: noteRealmForBreadcrumb.ancestorFolders ?? [],
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
        />
      </aside>
      <main
        class="daisy-flex-1 daisy-px-4 daisy-container daisy-mx-auto daisy-overflow-y-auto"
      >
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"
import { useRoute } from "vue-router"
import type { Folder, NoteRealm } from "@generated/doughnut-backend-api"
import { PanelLeft, PanelLeftClose } from "lucide-vue-next"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import Sidebar from "@/components/notes/Sidebar.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import {
  currentActiveNoteId,
  currentNotebookId,
  folderPageBreadcrumbFolders,
  notebookSidebarNotebookRealm,
  resetNotebookSidebarState,
} from "@/composables/useCurrentNoteSidebarState"

const route = useRoute()
const storageAccessor = useStorageAccessor()

const sidebarOpened = ref(false)
const windowWidth = ref(
  typeof window !== "undefined" ? window.innerWidth : 1024
)

const isMdOrLarger = computed(() => windowWidth.value >= 768)

const sidebarNotebookRealm = computed(() => notebookSidebarNotebookRealm.value)

const sidebarNotebookId = computed(() => currentNotebookId.value)

const sidebarRealm = computed((): NoteRealm | undefined => {
  const id = currentActiveNoteId.value
  if (id == null) return undefined
  return storageAccessor.value.refOfNoteRealm(id).value
})

const noteRealmForBreadcrumb = computed(() => {
  if (route.name === "notebookPage" || route.name === "folderPage") {
    return undefined
  }
  return sidebarRealm.value
})

const chromeBreadcrumbAncestorFolders = computed((): Folder[] => {
  if (route.name === "folderPage") {
    return folderPageBreadcrumbFolders.value
  }
  return []
})

const handleResize = () => {
  windowWidth.value = window.innerWidth
}

watch(
  () => currentNotebookId.value,
  () => {
    if (!isMdOrLarger.value) {
      sidebarOpened.value = false
    }
  }
)

watch(
  () => currentActiveNoteId.value,
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
