<template>
  <div class="mt-4" />
  <ContestableQuestion
    v-if="reviewQuestionInstance"
    v-bind="{ reviewQuestionInstance, storageAccessor }"
    @need-scroll="scrollToBottom"
  />
  <div
    class="chat-answer-container row"
    v-for="(message, index) in messages"
    :class="message.role"
    :key="index"
  >
    <div v-if="message.role === 'assistant'" class="assistant-icon col-auto">
      <SvgRobot />
    </div>
    <div class="col">
      <div
        v-if="message.role === 'assistant'"
        v-html="markdowntToHtml(message.content?.[0]?.text?.value)"
      />
      <div v-else class="d-flex justify-content-end">
        <div class="user-message" v-text="message.content?.[0]?.text?.value" />
      </div>
    </div>
  </div>

  <div ref="bottomOfTheChat" style="height: 140px; display: block"></div>

  <div class="chat-controls">
    <div class="container">
      <button
        v-if="!reviewQuestionInstance"
        class="btn btn-secondary"
        @click="generateQuestion"
      >
        Test me
      </button>
      <form class="chat-input-container" @submit.prevent="generateChatAnswer">
        <TextArea
          ref="chatInputTextArea"
          class="flex-grow-1"
          id="chat-input"
          :rows="1"
          :auto-extend-until="5"
          :enter-submit="true"
          v-model="chatInput"
          @enter-pressed="generateChatAnswer"
        />
        <input
          :disabled="isButtonDisabled"
          type="submit"
          value="Chat"
          id="chat-button"
          class="btn float-btn btn-secondary"
        />
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Message,
  Note,
  ChatRequest,
  MessageDelta,
  ReviewQuestionInstance,
} from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { PropType, computed, ref, onMounted } from "vue"
import markdownizer from "@/components/form/markdownizer"
import scrollToElement from "../commons/scrollToElement"

const { managedApi } = useLoadingApi()
const props = defineProps({
  selectedNote: { type: Object as PropType<Note>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
const reviewQuestionInstance = ref<ReviewQuestionInstance | undefined>(
  undefined
)
const chatInput = ref("")
const messages = ref<Message[]>([])
const bottomOfTheChat = ref<HTMLElement | null>(null)
const chatInputTextArea = ref<HTMLTextAreaElement | null>(null)
const threadId = ref<string | undefined>(undefined)

const isButtonDisabled = computed(() => chatInput.value === "")

const markdowntToHtml = (content?: string) =>
  markdownizer.markdownToHtml(content)
const scrollToBottom = () => {
  if (bottomOfTheChat.value) {
    scrollToElement(bottomOfTheChat.value)
  }
}

const generateQuestion = async () => {
  reviewQuestionInstance.value =
    await managedApi.restQuizQuestionController.generateQuestion(
      props.selectedNote.id
    )
  scrollToBottom()
}

const focusChatInput = () => {
  if (chatInputTextArea.value) {
    if (chatInputTextArea.value) {
      chatInputTextArea.value.focus()
    }
  }
}

const chat = async (id: Doughnut.ID, request: ChatRequest) => {
  await managedApi.eventSource
    .onMessage((event, data) => {
      if (event === "thread.message.created") {
        const response = JSON.parse(data) as Message
        response.content = [{ text: { value: "" } }]
        messages.value = [...messages.value, response]
        threadId.value = response.thread_id
      }
      if (event === "thread.message.delta") {
        const response = JSON.parse(data) as MessageDelta
        const message = messages.value[messages.value.length - 1]!
        const currentValue = message.content?.[0]?.text?.value
        const delta = response.delta?.content?.[0]?.text?.value
        message.content = [{ text: { value: currentValue! + delta } }]
      }
    })
    .onError((error) => {
      // eslint-disable-next-line no-console
      console.error(error)
    })
    .restAiController.chat(id, request)
}

const generateChatAnswer = async () => {
  messages.value.push({
    role: "user",
    content: [{ text: { value: chatInput.value } }],
  })
  const request = {
    userMessage: chatInput.value,
    threadId: threadId.value,
  }
  chatInput.value = ""
  focusChatInput()
  chat(props.selectedNote.id, request)
}

onMounted(() => {
  focusChatInput()
  managedApi.restAiController
    .tryRestoreChat(props.selectedNote.id)
    .then((response) => {
      messages.value = response
      if (response.length > 0) {
        threadId.value = response[response.length - 1]?.thread_id
      }
      scrollToBottom()
    })
})
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

input.auto-extendable-input {
  width: 100%;
}

.float-btn {
  float: right;
}

.user-message {
  background-color: #f0f0f0;
  border-radius: 10px;
  max-width: 70%;
  word-wrap: break-word;
  white-space: pre-wrap;
  text-align: left;
  padding: 12px 16px;
}
</style>
