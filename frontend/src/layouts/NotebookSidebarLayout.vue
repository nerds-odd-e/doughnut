<template>
  <div class="flex flex-col h-full">
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
          class="w-6 h-6"
          aria-hidden="true"
        />
        <PanelLeft
          v-else
          class="w-6 h-6"
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
      class="notebook-sidebar-drawer-backdrop fixed inset-x-0 bottom-0 bg-black/50 z-30"
      @click="sidebarOpened = false"
    />
    <div
      class="h-full relative flex flex-1 min-h-0"
    >
      <aside :class="sidebarClasses">
        <Sidebar
          v-if="currentNotebookId != null"
          :key="currentNotebookId"
          :active-note-realm="activeNoteRealm"
          :notebook-id="currentNotebookId"
          :notebook-readonly="currentNotebookRealm?.readonly === true"
          :active-folder-realm="activeFolderRealm"
          :breadcrumb-folders="breadcrumbFolders"
        />
      </aside>
      <main
        class="flex-1 px-4 container mx-auto overflow-y-auto"
      >
        <nav
          v-if="showRelocatedNoteCreationInMainColumn && currentNotebookId != null"
          :class="[noteChromeToolbarNavClass, 'overflow-visible mb-2']"
          data-testid="note-main-creation-toolbar"
        >
          <div class="daisy-btn-group daisy-btn-group-sm overflow-visible">
            <NoteCreationNewButton
              :notebook-id="currentNotebookId"
              :active-note-realm="activeNoteRealm"
              :active-folder-realm="activeFolderRealm"
              :breadcrumb-folders="breadcrumbFolders"
            />
          </div>
        </nav>
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
import { computed, inject, ref, watch, type Ref } from "vue"
import { RouterView, useRoute } from "vue-router"
import type {
  Folder,
  NotebookRealm,
  User,
} from "@generated/doughnut-backend-api"
import { PanelLeft, PanelLeftClose } from "@lucide/vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import Sidebar from "@/components/notes/Sidebar.vue"
import NoteCreationNewButton from "@/components/notes/NoteCreationNewButton.vue"
import { noteChromeToolbarNavClass } from "@/components/notes/noteChromeToolbarNavClass"
import { useStickyActiveNoteRealmForRoute } from "@/composables/useStickyActiveNoteRealmForRoute"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { useNotebookSidebarDrawer } from "@/composables/useNotebookSidebarDrawer"
import { useNotebookSidebarRouteRealms } from "@/composables/useNotebookSidebarRouteRealms"
import { useRelocatedNoteCreationInMainColumn } from "@/composables/useRelocatedNoteCreationInMainColumn"
import { useSidebarCreationReadonly } from "@/composables/useSidebarCreationReadonly"

const route = useRoute()
const storageAccessor = useStorageAccessor()
const currentUser = inject<Ref<User | undefined>>("currentUser")

const activeNoteRealm = useStickyActiveNoteRealmForRoute(route, storageAccessor)

const { activeNotebookRealm, activeFolderRealm, routeViewProps } =
  useNotebookSidebarRouteRealms(route)

const breadcrumbFolders = computed(
  (): Folder[] =>
    activeNoteRealm.value?.ancestorFolders ??
    activeFolderRealm.value?.ancestorFolders ??
    []
)

const currentNotebookRealm = computed(
  (): NotebookRealm | undefined =>
    activeNotebookRealm.value ??
    activeNoteRealm.value?.notebookRealm ??
    activeFolderRealm.value?.notebookRealm
)

const currentNotebookId = ref<number | undefined>(undefined)

watch(
  () => currentNotebookRealm.value?.notebook?.id,
  (id) => {
    if (typeof id !== "number" || !Number.isFinite(id)) return
    if (currentNotebookId.value !== id) {
      currentNotebookId.value = id
    }
  },
  { immediate: true }
)

const { sidebarOpened, isMdOrLarger, sidebarClasses } =
  useNotebookSidebarDrawer(route, currentNotebookId)

const noteCreationReadonly = useSidebarCreationReadonly(currentUser, () => ({
  activeNoteRealm: activeNoteRealm.value,
  notebookReadonly: currentNotebookRealm.value?.readonly === true,
}))

const showRelocatedNoteCreationInMainColumn =
  useRelocatedNoteCreationInMainColumn(
    sidebarOpened,
    route,
    currentNotebookId,
    noteCreationReadonly
  )
</script>

<style scoped lang="scss">
@use "@/assets/menu-variables.scss" as *;

.notebook-sidebar-drawer {
  @media (max-width: 768px) {
    top: calc(#{$main-menu-height-mobile} + env(safe-area-inset-top, 0px));
    bottom: 0;
    height: auto;
  }
}

.notebook-sidebar-drawer-backdrop {
  @media (max-width: 768px) {
    top: calc(#{$main-menu-height-mobile} + env(safe-area-inset-top, 0px));
  }
}

aside,
main {
  max-height: 100%;
}
</style>
