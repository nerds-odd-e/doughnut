<template>
  <div class="container">
    <ContentLoader v-if="!answeredQuestion" />
    <AnsweredQuestionComponent
      v-else
      v-bind="{ answeredQuestion, storageAccessor }"
    />
    <button
      v-if="answeredQuestion"
      class="conversation-button"
      title="Start a conversation about this question"
      @click="startConversation"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="24"
        height="24"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
      </svg>
    </button>
  </div>
</template>

<script setup lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import AnsweredQuestionComponent from "@/components/review/AnsweredQuestionComponent.vue"
import type { AnsweredQuestion } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { onMounted, ref, watch, type PropType } from "vue"
import { useRouter } from "vue-router"

const router = useRouter()
const { managedApi } = useLoadingApi()

const { recallPromptId } = defineProps({
  recallPromptId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
const answeredQuestion = ref<AnsweredQuestion | undefined>()

const fetchData = async () => {
  answeredQuestion.value =
    await managedApi.restRecallPromptController.showQuestion(recallPromptId)
}

const startConversation = async () => {
  if (!answeredQuestion.value) return

  const conversation =
    await managedApi.restConversationMessageController.startConversationAboutRecallPrompt(
      answeredQuestion.value.recallPromptId
    )

  router.push({
    name: "messageCenter",
    params: { conversationId: conversation.id },
  })
}

watch(() => recallPromptId, fetchData, { immediate: true })

onMounted(fetchData)
</script>

<style scoped>
.conversation-button {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: #4CAF50;
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 5px rgba(0,0,0,0.2);
  transition: background-color 0.3s;
}

.conversation-button:hover {
  background-color: #45a049;
}
</style>
