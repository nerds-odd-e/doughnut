<template>
  <div class="daisy-flex daisy-flex-col daisy-h-full">
    <ContentLoader v-if="resolvedNoteId === undefined" />
    <NoteShow
      v-else
      v-bind="{
        noteId: resolvedNoteId,
        expandChildren: true,
        isMinimized: isContentMinimized,
      }"
    >
      <template #note-conversation="{ noteRealm: conversationRealm }">
        <div
          v-if="Boolean(route.query.conversation)"
          class="conversation-wrapper daisy-border-t daisy-border-base-200 daisy-flex-1 daisy-flex daisy-flex-col daisy-bg-base-100/50"
        >
          <NoteConversation
            :note-id="conversationRealm.id"
            :is-maximized="isContentMinimized"
            @close-dialog="handleCloseConversation(conversationRealm)"
            @toggle-maximize="toggleMaximize"
          />
        </div>
      </template>
    </NoteShow>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted } from "vue"

import { useRoute, useRouter } from "vue-router"
import NoteShow from "../components/notes/NoteShow.vue"
import NoteConversation from "../components/conversations/NoteConversation.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import {
  currentActiveNoteId,
  currentNotebookId,
  notebookSidebarNotebookClientView,
} from "@/composables/useCurrentNoteSidebarState"
import { noteShowLocation } from "@/routes/noteShowLocation"
import type { NoteRealm } from "@generated/doughnut-backend-api"

const router = useRouter()
const route = useRoute()
const storageAccessor = useStorageAccessor()

const props = defineProps({
  noteId: { type: Number, required: false },
})

const resolvedNoteId = computed((): number | undefined => {
  if (props.noteId != null && !Number.isNaN(props.noteId)) {
    return props.noteId
  }
  return undefined
})

const noteRealm = computed(() => {
  const id = resolvedNoteId.value
  if (id == null) return undefined
  return storageAccessor.value.refOfNoteRealm(id).value
})

watch(
  resolvedNoteId,
  (id) => {
    if (id != null) {
      currentActiveNoteId.value = id
    }
  },
  { immediate: true }
)

watch(
  () => ({
    realm: noteRealm.value,
    noteId: props.noteId,
  }),
  ({ realm, noteId }) => {
    if (noteId != null && !Number.isNaN(noteId)) {
      const nb = realm?.notebookView.notebook.id
      if (nb != null) {
        currentNotebookId.value = nb
      }
      return
    }
    if (noteId == null || Number.isNaN(noteId)) {
      currentNotebookId.value = undefined
    }
  },
  { immediate: true }
)

const isContentMinimized = ref(false)

const toggleMaximize = () => {
  isContentMinimized.value = !isContentMinimized.value
}

const handleCloseConversation = (conversationRealm: NoteRealm) => {
  isContentMinimized.value = false
  router.replace({
    ...noteShowLocation(conversationRealm.id),
    query: {},
  })
}

onMounted(() => {
  notebookSidebarNotebookClientView.value = undefined
})
</script>

<style scoped>
.conversation-wrapper {
  max-height: 100%;
  overflow: hidden;
}
</style>
