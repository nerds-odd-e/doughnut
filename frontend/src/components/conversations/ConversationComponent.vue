<template>
  <div class="conversation-container">
    <!-- Upper half -->
    <div class="subject-container">
      <NoteShow v-if="conversation.subject?.note?.id" :noteId="conversation.subject?.note?.id" :storageAccessor="storageAccessor" :expandChildren="false" :readOnly="false" />
      <AssessmentQuestion v-else-if="conversation.subject?.assessmentQuestionInstance" :assessmentQuestionInstance="conversation.subject?.assessmentQuestionInstance" />
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
            class="card py-2 px-3"
            :class="[
              isCurrentUser(conversationMessage.sender?.id || 0) ? 'text-bg-dark' : 'bg-light',
              conversationMessage.sender?.id === undefined ? 'ai-chat' : '',
            ]"
          >
            <template v-if="conversationMessage.sender?.id === undefined">
              <SvgRobot />
            </template>
            {{ formatMessage(conversationMessage.message) }}
          </div>
        </div>
        <ScrollTo :scrollTrigger="currentConversationMessages.length" />
      </div>

      <div class="chat-controls">
        <form class="row chat-input-container" @submit.prevent="handleSendMessage()">
          <div class="col-md-10">
            <textarea class="w-100" name="Description" v-model="message" />
          </div>
          <div class="col-md-1">
            <input
              type="submit"
              value="Send"
              id="chat-button"
              class="btn float-btn btn-secondary"
            />
          </div>
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
  position: relative;
  display: flex;
  flex-direction: column;
  background-color: #f8f9fa;
  min-height: 0;
}

.messages-container {
  flex-grow: 1;
  overflow-y: auto;
  padding: 1rem;
  padding-bottom: 90px;
}

.chat-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: white;
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1);
  padding: 10px;
}

.ai-chat {
  color: red;
}
</style>
