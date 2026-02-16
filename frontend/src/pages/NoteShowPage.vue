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
          circle: noteRealm.notebook?.circle,
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
        v-bind="{
          noteRealm,
        }"
      />
    </aside>

    <!-- Main Content -->
    <main
      class="daisy-flex-1 daisy-px-4 daisy-container daisy-mx-auto daisy-overflow-y-auto"
    >
      <NoteShow
        v-bind="{
          noteId,
          expandChildren: true,
          isMinimized: isContentMinimized,
        }"
      >
        <template #note-conversation="{ noteRealm }">
          <div
            v-if="Boolean(route.query.conversation)"
            class="conversation-wrapper daisy-border-t daisy-border-base-200 daisy-flex-1 daisy-flex daisy-flex-col daisy-bg-base-100/50"
          >
            <NoteConversation
              :note-id="noteRealm.id"
              :is-maximized="isContentMinimized"
              @close-dialog="handleCloseConversation"
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
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import GlobalBar from "../components/toolbars/GlobalBar.vue"
import BreadcrumbWithCircle from "../components/toolbars/BreadcrumbWithCircle.vue"

const router = useRouter()
const route = useRoute()
const storageAccessor = useStorageAccessor()

const props = defineProps({
  noteId: { type: Number, required: true },
})

const noteRealm = computed(
  () => storageAccessor.value.refOfNoteRealm(props.noteId).value
)

const sidebarOpened = ref(false)
const isContentMinimized = ref(false)

const toggleMaximize = () => {
  isContentMinimized.value = !isContentMinimized.value
}

const handleCloseConversation = () => {
  router.replace({
    name: "noteShow",
    params: { noteId: props.noteId },
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

// Close sidebar automatically if noteId changes, to maintain a fresh state for each note
watch(
  () => props.noteId,
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
