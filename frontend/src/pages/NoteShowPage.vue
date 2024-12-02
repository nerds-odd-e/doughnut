<template>
  <div class="d-flex flex-grow-1 overflow-auto h-full">
    <aside
      class="flex-shrink-0 overflow-auto me-3 sidebar"
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
    <main class="flex-grow-1 overflow-visible main-content">
      <NoteShow
        v-bind="{
          noteId,
          expandChildren: true,
          storageAccessor,
          onToggleSidebar: () => sidebarCollapsed = !sidebarCollapsed,
        }"
      />
    </main>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, ref } from "vue"
import NoteShow from "../components/notes/NoteShow.vue"
import NoteSidebar from "../components/notes/NoteSidebar.vue"
import type { StorageAccessor } from "../store/createNoteStorage"

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

const sidebarCollapsed = ref(false)
</script>

<style scoped lang="scss">
@import '@/styles/_variables.scss';

aside {
  width: 18rem;
  transition: transform 0.3s ease;
}

.sidebar {
  @media (max-width: $mobile-breakpoint) {
    position: absolute;
    z-index: 1000;
    background: white;
    height: 100%;
    transform: translateX(0);

    &.sidebar-collapsed {
      transform: translateX(-100%);
    }
  }

  @media (min-width: $mobile-breakpoint) {
    &.sidebar-collapsed {
      width: 0;
      margin: 0;
      overflow: hidden;
    }
  }
}

.main-content {
  @media (max-width: $mobile-breakpoint) {
    margin-left: 0;

    .sidebar:not(.sidebar-collapsed) + & {
      display: none;
    }
  }
}

.h-full {
  height: 100%;
}
</style>
