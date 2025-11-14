<template>
  <div class="dialog-bar daisy-bg-base-300">
    <div class="daisy-flex daisy-align-items-center daisy-gap-2">
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
        class="daisy-btn daisy-btn-sm daisy-btn-outline-primary"
        @click="$emit('new-conversation')"
        aria-label="New Conversation"
      >
      +
      </button>
    </div>
    <div class="daisy-spacer"></div>
    <div class="daisy-flex daisy-align-items-center daisy-gap-2">
      <button
        class="export-button"
        @click="showExportDialog = true"
        aria-label="Export conversation"
        title="Export conversation to continue in external AI tools"
      >
        <SvgExport />
      </button>
      <button
        class="maximize-button"
        @click="$emit('toggle-maximize')"
        aria-label="Toggle maximize"
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
          <template v-if="isMaximized">
            <path d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3" />
          </template>
          <template v-else>
            <polyline points="15 3 21 3 21 9"></polyline>
            <polyline points="9 21 3 21 3 15"></polyline>
            <line x1="21" y1="3" x2="14" y2="10"></line>
            <line x1="3" y1="21" x2="10" y2="14"></line>
          </template>
        </svg>
      </button>
      <button
        class="minimize-button"
        @click="handleCloseDialog"
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
  </div>

  <div role="dialog" class="messages-container">
    <slot name="messages" />
  </div>

  <div class="bottom-container daisy-bg-base-100">
    <div v-if="defaultMessages" class="default-messages">
      <button
        v-for="(message, index) in defaultMessages"
        :key="index"
        class="default-message-button daisy-bg-base-200 daisy-text-base-content"
        @click="handleDefaultMessageClick(message)"
      >
        {{ message }}
      </button>
    </div>

    <div class="chat-controls daisy-bg-base-100">
      <form
        class="chat-input-form daisy-bg-base-200"
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
  </div>

  <ConversationExportDialog
    v-if="showExportDialog && selectedConversation"
    :conversation-id="selectedConversation.id"
    @close="showExportDialog = false"
  />
</template>

<script setup lang="ts">
import { ref, computed } from "vue"
import type { Conversation } from "@generated/backend"
import ConversationExportDialog from "./ConversationExportDialog.vue"
import SvgExport from "@/components/svgs/SvgExport.vue"

const { isMaximized, defaultMessages } = defineProps<{
  conversations?: Conversation[]
  selectedConversation?: Conversation
  allowNewConversation?: boolean
  isMaximized?: boolean
  defaultMessages?: string[]
}>()

const emit = defineEmits<{
  (e: "send-message", message: string): void
  (e: "send-message-and-invite-ai", message: string): void
  (e: "close-dialog"): void
  (e: "conversation-changed", conversationId: number): void
  (e: "new-conversation"): void
  (e: "toggle-maximize"): void
}>()

const message = ref("")
const showExportDialog = ref(false)

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

const handleCloseDialog = () => {
  if (isMaximized) {
    emit("toggle-maximize")
  }
  emit("close-dialog")
}

const handleDefaultMessageClick = (message: string) => {
  emit("send-message-and-invite-ai", message)
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
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1);
  padding: 1rem;
}

.chat-input-form {
  display: flex;
  align-items: center;
  gap: 0.5rem;
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
  background-color: hsl(var(--p) / 1);
  color: hsl(var(--pc) / 1);
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  padding: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.send-button:hover {
  background-color: hsl(var(--pf) / 1);
}

.send-button:disabled {
  background-color: hsl(var(--n) / 1);
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
  border-bottom: 1px solid hsl(var(--b3) / 1);
}

.minimize-button,
.maximize-button,
.export-button {
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
  border-radius: 4px;
}

.minimize-button:hover,
.maximize-button:hover,
.export-button:hover {
  background-color: hsl(var(--b3));
}

.conversation-select {
  padding: 0.25rem;
  border-radius: 4px;
  border: 1px solid hsl(var(--b3));
  background-color: hsl(var(--b1));
  font-size: 0.9rem;
}

.send-button.with-ai {
  background-color: hsl(var(--su) / 1);
}

.send-button.with-ai:hover {
  background-color: hsl(var(--su) / 1);
  opacity: 0.8;
}

.send-button.with-ai:disabled {
  background-color: hsl(var(--n) / 1);
}

.bottom-container {
  flex-shrink: 0;
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1);
  padding: 1rem;
}

.default-messages {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

@media (min-width: 768px) {
  .default-messages {
    grid-template-columns: 1fr 1fr;
  }
}

.default-message-button {
  text-align: left;
  padding: 0.75rem 1rem;
  background-color: hsl(var(--b2));
  border: 1px solid hsl(var(--b3));
  border-radius: 0.5rem;
  cursor: pointer;
  transition: background-color 0.2s;
  color: hsl(var(--nc));
}

.default-message-button:hover {
  background-color: hsl(var(--b3));
}
</style>
