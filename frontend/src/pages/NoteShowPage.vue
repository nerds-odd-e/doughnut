<template>
  <div class="d-flex flex-grow-1 overflow-auto h-full">
    <aside
      class="d-md-block flex-shrink-0 overflow-auto me-3"
      :class="{ 'd-none': sidebarCollapsedForSmallScreen }"
    >
      <NoteSidebar
        v-if="noteRealm"
        v-bind="{
          noteRealm,
          storageAccessor,
        }"
      />
    </aside>
    <main
      class="flex-grow-1 overflow-visible"
      :class="{ 'd-none': !sidebarCollapsedForSmallScreen }"
    >
      <NoteShow
        v-bind="{
          noteId,
          expandChildren: true,
          storageAccessor,
          onToggleSidebar: toggleSideBar,
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

const sidebarCollapsedForSmallScreen = ref(true)

const toggleSideBar = () => {
  sidebarCollapsedForSmallScreen.value = !sidebarCollapsedForSmallScreen.value
}
</script>

<style scoped lang="scss">
@import '@/styles/_variables.scss';

aside {
  width: 100%;
  @media (min-width: $mobile-breakpoint) {
    width: 18rem;
  }
}

.h-full {
  height: 100%;
}
</style>
