<template>
  <div class="dialog-bar">
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
      @submit.prevent="handleSendMessage()"
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
        @enter-pressed="handleSendMessage"
      />

      <button
        type="submit"
        role="button"
        class="send-button"
        aria-label="Send message"
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

const emit = defineEmits<{
  (e: "send-message", message: string): void
  (e: "close-dialog"): void
}>()

const message = ref("")

const trimmedMessage = computed(() => message.value.trim())

const handleSendMessage = async () => {
  if (!trimmedMessage.value) return
  emit("send-message", trimmedMessage.value)
  message.value = ""
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
</style>
