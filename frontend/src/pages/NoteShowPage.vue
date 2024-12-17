<template>
  <div class="daisy-flex daisy-flex-grow daisy-overflow-auto daisy-h-full daisy-relative">
    <aside
      class="daisy-flex-none daisy-overflow-auto daisy-px-3 sidebar"
      :class="{ 'sidebar-collapsed': sidebarCollapsed }"
    >
      <NoteSidebar
        v-if="noteRealm"
        v-bind="{
          noteRealm,
          storageAccessor,
        }"
      />
    </aside>
    <main class="daisy-flex-grow daisy-overflow-visible main-content">
      <NoteShow
        v-bind="{
          noteId,
          expandChildren: true,
          storageAccessor,
          onToggleSidebar: () => sidebarCollapsed = !sidebarCollapsed,
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
import { computed, ref } from "vue"
import NoteShow from "../components/notes/NoteShow.vue"
import NoteSidebar from "../components/notes/NoteSidebar.vue"
import type { StorageAccessor } from "../store/createNoteStorage"
import { useRoute, useRouter } from "vue-router"
import NoteConversation from "../components/conversations/NoteConversation.vue"

const router = useRouter()

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

const sidebarCollapsed = ref(window.innerWidth < 700)

const route = useRoute()

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
</script>

<style scoped lang="scss">
aside {
  width: 18rem;
  height: 100%;
  transition: transform 0.3s ease, width 0.3s ease, margin 0.3s ease;
}

.sidebar {
  @media (max-width: theme('screens.md')) {
    position: absolute;
    z-index: 1000;
    background: white;
    transform: translateX(-100%);

    &.sidebar-collapsed {
      transform: translateX(-100%);
    }

    &:not(.sidebar-collapsed) {
      transform: translateX(0);
    }
  }

  @media (min-width: theme('screens.md')) {
    transform: translateX(0);
    width: 18rem;

    &.sidebar-collapsed {
      width: 0;
      margin: 0;
      overflow: hidden;
    }
  }
}

.main-content {
  @media (max-width: theme('screens.md')) {
    margin-left: 0;

    .sidebar:not(.sidebar-collapsed) + & {
      display: none;
    }
  }
}
</style>
