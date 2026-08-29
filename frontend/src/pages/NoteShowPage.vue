<template>
  <div class="flex flex-col h-full">
    <ContentLoader v-if="resolvedNoteId === undefined" />
    <div
      v-else
      class="flex flex-col h-full min-h-0"
    >
      <NoteShow
        class="flex-1 min-h-0"
        v-bind="{
          noteId: resolvedNoteId,
          expandChildren: true,
          isMinimized: isContentMinimized,
          ownsShortcuts: true,
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
              @close-dialog="handleCloseConversation"
              @toggle-maximize="toggleMaximize"
            />
          </div>
        </template>
      </NoteShow>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue"

import { useRoute, useRouter } from "vue-router"
import NoteShow from "../components/notes/NoteShow.vue"
import NoteConversation from "../components/conversations/NoteConversation.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import { currentRouteSettingConversation } from "@/routes/noteShowLocation"
import { useAssimilationView } from "@/composables/useAssimilationView"

const router = useRouter()
const route = useRoute()
const { resetForNote } = useAssimilationView()

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

watch(
  resolvedNoteId,
  (id) => {
    if (id != null) {
      resetForNote(id)
    }
  },
  { immediate: true }
)

const toggleMaximize = () => {
  isContentMinimized.value = !isContentMinimized.value
}

const handleCloseConversation = () => {
  isContentMinimized.value = false
  router.replace(currentRouteSettingConversation(route, false))
}
</script>

<style scoped>
.conversation-wrapper {
  max-height: 100%;
  overflow: hidden;
}
</style>
