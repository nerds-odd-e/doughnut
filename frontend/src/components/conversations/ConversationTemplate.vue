<template>
  <div class="dialog-bar">
    <div class="d-flex align-items-center gap-2">
      <select
        v-if="conversations?.length && conversations.length > 1"
        class="conversation-select"
        :value="selectedConversation?.id"
        @change="handleConversationChange"
      >
        <option
          v-for="conv in conversations"
          :key="conv.id"
          :value="conv.id"
        >
          {{ `Conversation ${conv.id}` }}
        </option>
      </select>
      <button
        v-if="allowNewConversation"
        class="btn btn-sm btn-outline-primary"
        @click="$emit('new-conversation')"
        aria-label="New Conversation"
      >
      +
      </button>
    </div>
    <div class="spacer"></div>
    <button
      class="minimize-button"
      @click="$emit('close-dialog')"
      aria-label="Close dialog"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="20"
        height="20"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <line x1="5" y1="12" x2="19" y2="12"></line>
      </svg>
    </button>
  </div>

  <div class="messages-container">
    <slot name="messages" />
  </div>

  <div class="chat-controls">
    <form
      class="chat-input-form"
      @submit.prevent="handleSendMessageWithAI()"
      :disabled="!trimmedMessage"
    >
      <TextArea
        ref="chatInputTextArea"
        v-focus
        class="chat-input"
        id="chat-input"
        :rows="1"
        :auto-extend-until="5"
        :enter-submit="true"
        v-model="message"
        @enter-pressed="handleSendMessageWithAI"
      />

      <button
        type="submit"
        role="button"
        class="send-button with-ai"
        aria-label="Send message and invite AI to reply"
        :disabled="!trimmedMessage"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z"/>
          <path d="M12 8v8"/>
          <path d="M5 3a2 2 0 0 0-2 2v2c0 1.1.9 2 2 2"/>
          <path d="M19 3a2 2 0 0 1 2 2v2c0 1.1-.9 2-2 2"/>
          <path d="M12 16a2 2 0 0 0-2 2v2a2 2 0 0 0 4 0v-2a2 2 0 0 0-2-2z"/>
          <path d="M4 19a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2"/>
        </svg>
      </button>
      <button
        type="button"
        role="button"
        class="send-button"
        aria-label="Send message"
        @click="handleSendMessage()"
        :disabled="!trimmedMessage"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13"></line>
          <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
        </svg>
      </button>

    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue"
import type { Conversation } from "@/generated/backend"

defineProps<{
  conversations?: Conversation[]
  selectedConversation?: Conversation
  allowNewConversation?: boolean
}>()

const emit = defineEmits<{
  (e: "send-message", message: string): void
  (e: "send-message-and-invite-ai", message: string): void
  (e: "close-dialog"): void
  (e: "conversation-changed", conversationId: number): void
  (e: "new-conversation"): void
}>()

const message = ref("")

const trimmedMessage = computed(() => message.value.trim())

const handleSendMessage = async (withAI: boolean = false) => {
  if (!trimmedMessage.value) return

  if (withAI) {
    emit("send-message-and-invite-ai", trimmedMessage.value)
  } else {
    emit("send-message", trimmedMessage.value)
  }

  message.value = ""
}

const handleSendMessageWithAI = () => {
  handleSendMessage(true)
}

const handleConversationChange = (event: Event) => {
  const select = event.target as HTMLSelectElement
  emit("conversation-changed", parseInt(select.value))
}
</script>

<style scoped>
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

.send-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.chat-input-form[disabled] {
  opacity: 0.7;
  cursor: not-allowed;
}

.dialog-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 1rem;
  background-color: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
}

.minimize-button {
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
  border-radius: 4px;
}

.minimize-button:hover {
  background-color: #e9ecef;
}

.conversation-select {
  padding: 0.25rem;
  border-radius: 4px;
  border: 1px solid #dee2e6;
  background-color: white;
  font-size: 0.9rem;
}

.send-button.with-ai {
  background-color: #198754;  /* Bootstrap's success color */
}

.send-button.with-ai:hover {
  background-color: #157347;
}

.send-button.with-ai:disabled {
  background-color: #ccc;
}
</style>
