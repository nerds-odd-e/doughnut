<template>
  <ContestableQuestion
    v-if="recallPrompt"
    v-bind="{ recallPrompt, storageAccessor }"
    @need-scroll="scrollToBottom"
  />

  <div ref="bottomOfTheChat" style="height: 140px; display: block"></div>
</template>

<script setup lang="ts">
import type { Note, RecallPrompt } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { ref, onMounted } from "vue"
import scrollToElement from "../commons/scrollToElement"

const { managedApi } = useLoadingApi()
const { selectedNote, storageAccessor } = defineProps({
  selectedNote: { type: Object as PropType<Note>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
const recallPrompt = ref<RecallPrompt | undefined>(undefined)
const bottomOfTheChat = ref<HTMLElement | null>(null)

const scrollToBottom = () => {
  if (bottomOfTheChat.value) {
    scrollToElement(bottomOfTheChat.value)
  }
}

const generateQuestion = async () => {
  recallPrompt.value =
    await managedApi.restRecallPromptController.generateQuestion(
      selectedNote.id
    )
  scrollToBottom()
}

onMounted(() => {
  generateQuestion()
})
</script>
