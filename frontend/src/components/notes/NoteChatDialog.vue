<template>
  <div class="mt-4"/>
  <ContestableQuestion
    v-if="quizQuestionInNotebook"
    v-bind="{ quizQuestionInNotebook, storageAccessor }"
    @need-scroll="scrollToBottom"
  />
  <div
    class="chat-answer-container row"
    v-for="(message, index) in messages"
    :class="message.role"
    :key="index"
  >
    <div v-if="message.role==='assistant'" class="assistant-icon col-auto">
      <SvgRobot />
    </div>
    <div class="col">
      <div v-if="message.role==='assistant'" v-html="markdowntToHtml(message.content?.[0]?.text?.value)"/>
      <div v-else class="chat-message-content">
        {{ message.content?.[0]?.text?.value }}
      </div>
    </div>
  </div>

  <div ref="bottomOfTheChat" style="height: 140px; display: block"></div>

  <div class="chat-controls">
    <div class="container">
      <button
        v-if="!quizQuestionInNotebook"
        class="btn btn-secondary"
        @click="generateQuestion"
      >
        Test me
      </button>
      <form class="chat-input-container" @submit.prevent="generateChatAnswer">
        <input id="chat-input" class="chat-input-text" v-model="chatInput" />
        <input
          :disabled="isButtonDisabled"
          type="submit"
          value="Chat"
          id="chat-button"
          class="btn float-btn btn-secondary"
        />
      </form>
      <p class="text-center">
        <i
          >Ask question to AI about this note. Each question ignores the chat
          history, unlike ChatGPT.</i
        >
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Message, Note, QuizQuestionInNotebook } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { PropType, computed, ref } from "vue"
import markdownizer from "@/components/form/markdownizer"
import scrollToElement from "../commons/scrollToElement"
import ContestableQuestion from "../review/ContestableQuestion.vue"
import SvgRobot from "../svgs/SvgRobot.vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  selectedNote: { type: Object as PropType<Note>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
const quizQuestionInNotebook = ref<QuizQuestionInNotebook | undefined>(undefined)
const chatInput = ref("")
const messages = ref<Message[]>([])
const bottomOfTheChat = ref<HTMLElement | null>(null)

const isButtonDisabled = computed(() => chatInput.value === "")
const threadId = computed(() => messages.value?.[messages.value.length - 1]?.thread_id)

const markdowntToHtml = (content?: string) => markdownizer.markdownToHtml(content)
const scrollToBottom = () => {
  if (bottomOfTheChat.value) {
    scrollToElement(bottomOfTheChat.value)
  }
}

const generateQuestion = async () => {
  quizQuestionInNotebook.value = await managedApi.restQuizQuestionController.generateQuestion(
    props.selectedNote.id,
  )
  scrollToBottom()
}

const generateChatAnswer = async () => {
  messages.value.push({
    role: "user",
    content: [{ text: { value: chatInput.value } }],
  })
  messages.value = [...messages.value, ...(await managedApi.restAiController.chat(props.selectedNote.id, {
    userMessage: chatInput.value,
    threadId: threadId.value,
  })).messages!]
}
</script>

<style lang="scss" scoped>
span {
  display: block;
  overflow: hidden;
  padding-right: 5px;
}

.chat-controls {
  position: fixed;
  bottom: 0;
  width: 100%;
  background-color: white;
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1);
}

.chat-input-container {
  width: 100%;
  display: flex;
  flex-direction: row;
  padding-top: 5px;
  padding-bottom: 5px;
}

.chat-input-text {
  width: 100%;
  margin-right: 5px;
  flex-grow: 1;
}
input.auto-extendable-input {
  width: 100%;
}

.float-btn {
  float: right;
}

.user .chat-message-content {
  background-color: #f0f0f0;
  border-radius: 10px;
  padding: 10px;
  margin: 5px;
  max-width: 70%;
  float: right;
}
</style>
