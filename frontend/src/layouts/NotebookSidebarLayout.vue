<template>
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
      <BreadcrumbWithCircle
        v-if="noteRealmForBreadcrumb"
        v-bind="{
          fromBazaar: noteRealmForBreadcrumb.fromBazaar,
          circle: notebookForBreadcrumb?.circle,
          noteTopology: noteRealmForBreadcrumb.note.noteTopology,
          ancestorFolders: noteRealmForBreadcrumb.ancestorFolders ?? [],
        }"
      />
      <div
        v-else-if="notebookPageContext"
        class="daisy-text-sm daisy-breadcrumbs daisy-max-w-full daisy-min-w-0"
      >
        <ul class="daisy-m-0 daisy-pl-0">
          <li v-if="notebookPageContext.isNotebookReadOnly">
            <router-link :to="{ name: 'bazaar' }">Bazaar</router-link>
          </li>
          <template v-else>
            <li>
              <router-link :to="{ name: 'notebooks' }">Notebooks</router-link>
            </li>
            <li v-if="notebookPageContext.notebook.circle">
              <router-link
                :to="{
                  name: 'circleShow',
                  params: { circleId: notebookPageContext.notebook.circle.id },
                }"
                >{{ notebookPageContext.notebook.circle.name }}</router-link
              >
            </li>
          </template>
          <li>{{ notebookPageContext.notebook.name }}</li>
        </ul>
      </div>
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
          'daisy-bg-base-200 daisy-w-72 daisy-transition-all daisy-ease-in-out daisy-overflow-y-auto',
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
        <NoteSidebar
          v-if="sidebarNotebookId != null && !Number.isNaN(sidebarNotebookId)"
          :note-realm="sidebarRealm"
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
import type { NoteRealm, Notebook } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import NoteSidebar from "@/components/notes/NoteSidebar.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import {
  currentActiveNoteId,
  currentNotebookId,
  notebookSidebarNotebookPageContext,
  resetNotebookSidebarState,
} from "@/composables/useCurrentNoteSidebarState"

const route = useRoute()
const storageAccessor = useStorageAccessor()

const sidebarOpened = ref(false)
const windowWidth = ref(
  typeof window !== "undefined" ? window.innerWidth : 1024
)

const isMdOrLarger = computed(() => windowWidth.value >= 768)

const notebookPageContext = computed(
  () => notebookSidebarNotebookPageContext.value
)

const sidebarNotebookId = computed(() => currentNotebookId.value)

const sidebarRealm = computed((): NoteRealm | undefined => {
  const id = currentActiveNoteId.value
  if (id == null) return undefined
  return storageAccessor.value.refOfNoteRealm(id).value
})

const noteRealmForBreadcrumb = computed(() => {
  if (route.name === "notebookPage") {
    return undefined
  }
  return sidebarRealm.value
})

const notebookForBreadcrumb = ref<Notebook | undefined>(undefined)

watch(
  () => noteRealmForBreadcrumb.value?.notebookId ?? currentNotebookId.value,
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
      notebookForBreadcrumb.value = data.notebook
    }
  },
  { immediate: true }
)

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
