<template>
  <div class="conversation-container">
    <!-- Upper half -->
    <div class="subject-container">
      <NoteShow
        v-if="conversation.subject?.note?.id"
        v-bind="{
          noteId: conversation.subject?.note?.id,
          storageAccessor,
          expandChildren: false,
          readonly: false
        }"
      />
      <AssessmentQuestion
        v-else-if="conversation.subject?.assessmentQuestionInstance"
        v-bind="{
          assessmentQuestionInstance: conversation.subject?.assessmentQuestionInstance
        }"
      />
    </div>

    <!-- Lower half -->
    <div class="conversation-messages">
      <div class="messages-container" v-if="currentConversationMessages">
        <div
          v-for="conversationMessage in currentConversationMessages"
          :key="conversationMessage.id"
          class="d-flex mb-3"
          :class="{ 'justify-content-end': isCurrentUser(conversationMessage.sender?.id || 0) }"
        >
          <div
            v-if="!isCurrentUser(conversationMessage.sender?.id || 0)"
            class="message-avatar me-2"
            :title="conversationMessage.sender?.name || 'AI Assistant'"
          >
            <template v-if="conversationMessage.sender?.id === undefined">
              <SvgRobot />
            </template>
            <template v-else>
              <SvgMissingAvatar />
            </template>
          </div>

          <div
            class="card py-2 px-3"
            :class="[
              isCurrentUser(conversationMessage.sender?.id || 0) ? 'text-bg-dark' : 'bg-light',
              conversationMessage.sender?.id === undefined ? 'ai-chat' : '',
            ]"
          >
            {{ formatMessage(conversationMessage.message) }}
          </div>
        </div>
        <ScrollTo :scrollTrigger="currentConversationMessages.length" />
      </div>

      <div class="chat-controls">
        <form class="chat-input-form" @submit.prevent="handleSendMessage()">
          <TextArea
            ref="chatInputTextArea"
            v-focus
            class="chat-input"
            id="chat-input"
            :rows="1"
            :auto-extend-until="5"
            :enter-submit="true"
            v-model="message"
            @enter-pressed="handleSendMessage"
          />

          <button
            type="submit"
            class="send-button"
            aria-label="Send message"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type {
  User,
  ConversationMessage,
  Conversation,
} from "@/generated/backend"
import SvgRobot from "@/components/svgs/SvgRobot.vue"
import NoteShow from "@/components/notes/NoteShow.vue"
import AssessmentQuestion from "@/components/assessment/AssessmentQuestion.vue"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import SvgMissingAvatar from "@/components/svgs/SvgMissingAvatar.vue"

const props = defineProps<{
  conversation: Conversation
  user: User
  storageAccessor: StorageAccessor
}>()

const emit = defineEmits<{
  (e: "conversation-fetched", conversationId: number): void
}>()

const { managedApi } = useLoadingApi()

const currentConversationMessages = ref<ConversationMessage[] | undefined>(
  undefined
)
const message = ref("")

const formatMessage = (message: string) => {
  return message.replace(/^"|"$/g, "").trim()
}

const isCurrentUser = (id: number): boolean => {
  return id === props.user?.id
}

const fetchConversationMessages = async () => {
  currentConversationMessages.value =
    await managedApi.restConversationMessageController.getConversationMessages(
      props.conversation.id
    )
  emit("conversation-fetched", props.conversation.id)
}

const handleSendMessage = async () => {
  await managedApi.restConversationMessageController.replyToConversation(
    props.conversation.id,
    message.value
  )
  message.value = ""
  await fetchConversationMessages()
}

onMounted(() => {
  fetchConversationMessages()
})

watch(() => props.conversation, fetchConversationMessages)
</script>

<style scoped>
.conversation-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.subject-container {
  flex: 1;
  overflow-y: auto;
  border-bottom: 1px solid #dee2e6;
  padding: 1rem;
}

.conversation-messages {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f8f9fa;
  min-height: 0;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}

.chat-controls {
  flex-shrink: 0;
  background-color: white;
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1);
  padding: 1rem;
}

.chat-input-form {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 0.5rem;
}

.chat-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 0.5rem;
  resize: none;
}

.chat-input:focus {
  outline: none;
}

.send-button {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #0d6efd;
  color: white;
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  padding: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.send-button:hover {
  background-color: #0b5ed7;
}

.ai-chat {
  color: red;
}

.message-avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
