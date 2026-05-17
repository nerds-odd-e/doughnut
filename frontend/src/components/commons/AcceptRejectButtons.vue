<template>
  <div class="daisy-card py-2 px-3 bg-base-200 ai-chat">
    <div>
      <slot name="title" />
    </div>
    <div class="suggestion-content mb-2">
      <slot name="content" />
    </div>
    <div class="flex gap-2">
      <button
        v-if="!hideAccept"
        class="daisy-btn daisy-btn-primary daisy-btn-sm"
        @click="$emit('accept')"
        :disabled="disabled"
      >
        Accept
      </button>
      <button
        class="daisy-btn daisy-btn-outline daisy-btn-secondary daisy-btn-sm"
        @click="$emit('skip')"
        :disabled="disabled"
      >
        Skip
      </button>
      <button
        class="daisy-btn daisy-btn-secondary daisy-btn-sm"
        @click="$emit('cancel')"
        :disabled="disabled"
      >
        Cancel
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  disabled?: boolean
  hideAccept?: boolean
}>()

defineEmits<{
  (e: "accept"): void
  (e: "cancel"): void
  (e: "skip"): void
}>()
</script>

<style scoped>
.ai-chat {
  background-color: #f8f9fa;
  border-left: 3px solid #0d6efd;
}

.suggestion-content {
  font-style: italic;
  color: #666;
}
</style>
