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
      <ConversationInner
        v-bind="{ conversation, user, storageAccessor }"
        @conversation-fetched="emit('conversation-fetched', $event)"
        @conversation-created="onConversationCreated($event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { User, Conversation } from "@/generated/backend"
import NoteShow from "@/components/notes/NoteShow.vue"
import AssessmentQuestion from "@/components/assessment/AssessmentQuestion.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { useRouter } from "vue-router"

const router = useRouter()

defineProps<{
  conversation: Conversation
  user: User
  storageAccessor: StorageAccessor
}>()

const emit = defineEmits<{
  (e: "conversation-fetched", conversationId: number): void
}>()

const onConversationCreated = async (newConversation: Conversation) => {
  router.push({
    name: "messageCenter",
    params: { conversationId: newConversation.id },
  })
  return
}
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
