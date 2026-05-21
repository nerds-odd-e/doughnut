<template>
  <div class="flex flex-col h-full">
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
          class="conversation-wrapper border-t border-base-200 flex-1 flex flex-col bg-base-100/50"
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
import { ref, computed } from "vue"

import { useRoute, useRouter } from "vue-router"
import NoteShow from "../components/notes/NoteShow.vue"
import NoteConversation from "../components/conversations/NoteConversation.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import { noteShowLocation } from "@/routes/noteShowLocation"
import type { NoteRealm } from "@generated/doughnut-backend-api"

const router = useRouter()
const route = useRoute()

const props = defineProps({
  noteId: { type: Number, required: false },
})

const resolvedNoteId = computed((): number | undefined => {
  if (props.noteId != null && !Number.isNaN(props.noteId)) {
    return props.noteId
  }
  return undefined
})

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
</script>

<style scoped>
.conversation-wrapper {
  max-height: 100%;
  overflow: hidden;
}
</style>
