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
              @close-dialog="handleCloseConversation(conversationRealm)"
              @toggle-maximize="toggleMaximize"
            />
          </div>
        </template>
      </NoteShow>
      <div
        v-if="noteForPanel"
        :key="`${noteForPanel.id}-${panelKey}`"
        class="shrink-0"
      >
        <AssimilationPanel
          :note="noteForPanel"
          @reload-needed="onAssimilationReloadNeeded"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue"

import { useRoute, useRouter } from "vue-router"
import NoteShow from "../components/notes/NoteShow.vue"
import NoteConversation from "../components/conversations/NoteConversation.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import AssimilationPanel from "@/components/recall/AssimilationPanel.vue"
import { noteShowLocation } from "@/routes/noteShowLocation"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const router = useRouter()
const route = useRoute()
const storageAccessor = useStorageAccessor()
const { showAssimilationSettings, resetForNote } = useAssimilationView()

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
const panelKey = ref(0)

watch(
  resolvedNoteId,
  (id) => {
    if (id != null) {
      resetForNote(id)
    }
  },
  { immediate: true }
)

const noteForPanel = computed(() => {
  const id = resolvedNoteId.value
  if (id == null || !showAssimilationSettings.value) {
    return undefined
  }
  return storageAccessor.value.refOfNoteRealm(id).value?.note
})

const onAssimilationReloadNeeded = async () => {
  const id = resolvedNoteId.value
  if (id == null) {
    return
  }
  await storageAccessor.value.storedApi().loadNoteRealm(id)
  panelKey.value += 1
}

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
