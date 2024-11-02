<template>
  <ContestableQuestion
    v-if="reviewQuestionInstance"
    v-bind="{ reviewQuestionInstance, storageAccessor }"
    @need-scroll="scrollToBottom"
  />

  <div ref="bottomOfTheChat" style="height: 140px; display: block"></div>
</template>

<script setup lang="ts">
import type { Note, ReviewQuestionInstance } from "@/generated/backend"
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
const reviewQuestionInstance = ref<ReviewQuestionInstance | undefined>(
  undefined
)
const bottomOfTheChat = ref<HTMLElement | null>(null)

const scrollToBottom = () => {
  if (bottomOfTheChat.value) {
    scrollToElement(bottomOfTheChat.value)
  }
}

const generateQuestion = async () => {
  reviewQuestionInstance.value =
    await managedApi.restReviewQuestionController.generateQuestion(
      selectedNote.id
    )
  scrollToBottom()
}

onMounted(() => {
  generateQuestion()
})
</script>
