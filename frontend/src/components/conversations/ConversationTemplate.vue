<template>
  <div class="dialog-bar bg-base-300">
    <div class="flex items-center gap-2">
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
        class="daisy-btn daisy-btn-sm daisy-btn-outline daisy-btn-primary"
        @click="$emit('new-conversation')"
        aria-label="New Conversation"
      >
      +
      </button>
    </div>
    <div class="spacer"></div>
    <div class="flex items-center gap-2">
      <button
        class="export-button"
        @click="showExportDialog = true"
        aria-label="Export conversation"
        title="Export conversation to continue in external AI tools"
      >
        <Upload class="w-6 h-6" />
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

  <ConversationMessageComposer
    :default-messages="defaultMessages"
    @send-message="$emit('send-message', $event)"
    @send-message-and-invite-ai="$emit('send-message-and-invite-ai', $event)"
  />

  <ConversationExportDialog
    v-if="showExportDialog && selectedConversation"
    :conversation-id="selectedConversation.id"
    @close="showExportDialog = false"
  />
</template>

<script setup lang="ts">
import { ref } from "vue"
import type { Conversation } from "@generated/doughnut-backend-api"
import ConversationExportDialog from "./ConversationExportDialog.vue"
import ConversationMessageComposer from "./ConversationMessageComposer.vue"
import { Upload } from "@lucide/vue"

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

const showExportDialog = ref(false)

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
</script>

<style scoped>
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}

.dialog-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 1rem;
  border-bottom: 1px solid var(--color-base-300);
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
  background-color: var(--color-base-300);
}

.conversation-select {
  padding: 0.25rem;
  border-radius: 4px;
  border: 1px solid var(--color-base-300);
  background-color: var(--color-base-100);
  font-size: 0.9rem;
}
</style>
