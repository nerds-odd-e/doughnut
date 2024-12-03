<template>
  <div v-if="note">
    <fieldset
      class="note-under-question"
    >
      <legend>Note under question</legend>
      <Breadcrumb v-bind="{ noteTopic: note.noteTopic, includingSelf: true }" />
    </fieldset>
  </div>
  <QuestionDisplay
    v-if="answeredQuestion.predefinedQuestion"
    v-bind="{
      bareQuestion: answeredQuestion.predefinedQuestion.bareQuestion,
      correctChoiceIndex: answeredQuestion.predefinedQuestion.correctAnswerIndex,
      answer: answeredQuestion.answer,
    }"
  />
  <AnswerResult v-bind="{ answeredQuestion }" />
  <button
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
</template>

<script setup lang="ts">
import type { AnsweredQuestion } from "@/generated/backend"
import type { PropType } from "vue"
import QuestionDisplay from "./QuestionDisplay.vue"
import { useRouter } from "vue-router"
import useLoadingApi from "@/managedApi/useLoadingApi"
import Breadcrumb from "@/components/toolbars/Breadcrumb.vue"

const router = useRouter()
const { managedApi } = useLoadingApi()

const { answeredQuestion } = defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
})

const note = answeredQuestion?.note

const startConversation = async () => {
  const conversation =
    await managedApi.restConversationMessageController.startConversationAboutRecallPrompt(
      answeredQuestion.recallPromptId
    )

  router.push({
    name: "messageCenter",
    params: { conversationId: conversation.id },
  })
}
</script>

<style lang="sass" scoped>
.note-under-question
  border: 1px solid #ccc
  width: 100%
  border-radius: 5px
  padding: 8px 12px
  margin-top: 10px
  background-color: #f9f9f9

.conversation-button
  position: fixed
  bottom: 20px
  right: 20px
  width: 48px
  height: 48px
  border-radius: 50%
  background-color: #4CAF50
  color: white
  border: none
  cursor: pointer
  display: flex
  align-items: center
  justify-content: center
  box-shadow: 0 2px 5px rgba(0,0,0,0.2)
  transition: background-color 0.3s

  &:hover
    background-color: #45a049
</style>
