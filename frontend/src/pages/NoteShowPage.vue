<template>
  <div class="daisy-drawer daisy-lg:drawer-open daisy-h-full daisy-relative">
    <input id="drawer" type="checkbox" class="daisy-drawer-toggle" v-model="sidebarOpened" />

    <!-- Drawer content -->
    <div class="daisy-drawer-content daisy-overflow-visible">
      <main class="daisy-container daisy-mx-auto daisy-p-4 daisy-overflow-visible">
        <NoteShow
          v-bind="{
            noteId,
            expandChildren: true,
            storageAccessor,
            onToggleSidebar: () => sidebarOpened = !sidebarOpened,
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

    <!-- Drawer side -->
    <div class="daisy-drawer-side daisy-absolute !daisy-h-full">
      <label for="drawer" class="daisy-drawer-overlay"></label>
      <aside class="daisy-w-72 daisy-h-full daisy-bg-base-200">
        <NoteSidebar
          v-if="noteRealm"
          v-bind="{
            noteRealm,
            storageAccessor,
          }"
        />
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, ref, watch } from "vue"
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

const sidebarOpened = ref(false)

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

// Add watcher for noteId
watch(
  () => props.noteId,
  () => {
    sidebarOpened.value = false
  }
)
</script>

<style scoped>
.daisy-drawer-side {
  height: 100% !important;
  min-height: unset !important;
}
</style>
