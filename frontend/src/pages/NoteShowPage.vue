<template>
  <!--
    Instead of using a daisy:drawer, we're using a responsive layout with a sidebar that
    is either a static column on md+ screens or an overlay on smaller screens.
  -->
  <div class="daisy:h-full daisy:relative daisy:flex">
    <!-- Overlay mask for mobile -->
    <div
      v-if="!isMdOrLarger && sidebarOpened"
      class="daisy:fixed daisy:inset-0 daisy:bg-black/50 daisy:z-30"
      @click="sidebarOpened = false"
    ></div>

    <!-- Sidebar -->
    <aside
      :class="[
        'daisy:bg-base-200 daisy:w-72 daisy:transition-all daisy:ease-in-out daisy:overflow-y-auto',
        isMdOrLarger
          ? (sidebarOpened ? 'daisy:relative' : 'daisy:hidden')
          : (sidebarOpened
              ? 'daisy:translate-x-0 daisy:fixed daisy:top-0 daisy:left-0 daisy:z-40 daisy:h-full'
              : 'daisy:-translate-x-full daisy:fixed daisy:top-0 daisy:left-0 daisy:h-full')
      ]"
    >
      <NoteSidebar
        v-if="noteRealm"
        v-bind="{
          noteRealm,
          storageAccessor,
        }"
      />
    </aside>

    <!-- Main Content -->
    <main
      class="daisy:flex-1 daisy:p-4 daisy:container daisy:mx-auto daisy:overflow-y-auto"
    >
      <NoteShow
        v-bind="{
          noteId,
          expandChildren: true,
          storageAccessor,
          onToggleSidebar: () => sidebarOpened = !sidebarOpened,
          isMinimized: isContentMinimized,
          isSidebarExpanded: sidebarOpened,
        }"
      >
        <template #note-conversation="{ noteRealm }">
          <div
            v-if="Boolean(route.query.conversation)"
            class="conversation-wrapper daisy:border-t daisy:border-base-200 daisy:flex-1 daisy:flex daisy:flex-col daisy:bg-base-100/50"
          >
            <NoteConversation
              :note-id="noteRealm.id"
              :storage-accessor="storageAccessor"
              :is-maximized="isContentMinimized"
              @close-dialog="handleCloseConversation"
              @toggle-maximize="toggleMaximize"
            />
          </div>
        </template>
      </NoteShow>
    </main>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref, watch, computed, onMounted, onBeforeUnmount } from "vue"

import { useRoute, useRouter } from "vue-router"
import NoteShow from "../components/notes/NoteShow.vue"
import NoteSidebar from "../components/notes/NoteSidebar.vue"
import NoteConversation from "../components/conversations/NoteConversation.vue"

import type { StorageAccessor } from "../store/createNoteStorage"

const router = useRouter()
const route = useRoute()

const props = defineProps({
  noteId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const noteRealm = computed(() => {
  return props.storageAccessor.refOfNoteRealm(props.noteId).value
})

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
