<template>
  <div class="bottom-container bg-base-100">
    <div v-if="defaultMessages" class="default-messages">
      <button
        v-for="(preset, index) in defaultMessages"
        :key="index"
        class="default-message-button bg-base-200 text-base-content"
        @click="emit('send-message-and-invite-ai', preset)"
      >
        {{ preset }}
      </button>
    </div>

    <div class="message-controls bg-base-100">
      <form
        class="message-input-form bg-base-200"
        @submit.prevent="send(true)"
        :disabled="!trimmedMessage"
      >
        <TextArea
          v-focus
          class="message-input"
          id="message-input"
          :rows="1"
          :auto-extend-until="5"
          :enter-submit="true"
          v-model="message"
          @enter-pressed="send(true)"
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
          @click="send(false)"
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
</template>

<script setup lang="ts">
import { computed, ref } from "vue"

defineProps<{
  defaultMessages?: string[]
}>()

const emit = defineEmits<{
  (e: "send-message", message: string): void
  (e: "send-message-and-invite-ai", message: string): void
}>()

const message = ref("")
const trimmedMessage = computed(() => message.value.trim())

const send = (inviteAi: boolean) => {
  if (!trimmedMessage.value) return
  if (inviteAi) {
    emit("send-message-and-invite-ai", trimmedMessage.value)
  } else {
    emit("send-message", trimmedMessage.value)
  }
  message.value = ""
}
</script>

<style scoped>
.message-controls {
  flex-shrink: 0;
}

.message-input-form {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border-radius: 8px;
  padding: 0.5rem;
}

.message-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 0.25rem;
  resize: none;
}

.message-input:focus {
  outline: none;
}

.send-button {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--color-primary);
  color: var(--color-primary-content);
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  padding: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.send-button:hover {
  background-color: color-mix(in oklch, var(--color-primary), black 12%);
}

.send-button:disabled {
  background-color: var(--color-neutral);
  cursor: not-allowed;
}

.message-input-form[disabled] {
  opacity: 0.7;
  cursor: not-allowed;
}

.send-button.with-ai {
  background-color: var(--color-success);
}

.send-button.with-ai:hover {
  background-color: var(--color-success);
  opacity: 0.8;
}

.send-button.with-ai:disabled {
  background-color: var(--color-neutral);
}

.bottom-container {
  flex-shrink: 0;
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1);
  padding: 0.5rem;
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
  background-color: var(--color-base-200);
  border: 1px solid var(--color-base-300);
  border-radius: 0.5rem;
  cursor: pointer;
  transition: background-color 0.2s;
  color: var(--color-neutral-content);
}

.default-message-button:hover {
  background-color: var(--color-base-300);
}
</style>
