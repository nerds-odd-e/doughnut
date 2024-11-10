<template>
  <div class="card py-2 px-3 bg-light ai-chat">
    <div>
      <slot name="title" />
    </div>
    <div class="suggestion-content mb-2">
      <slot name="content" />
    </div>
    <div class="d-flex gap-2">
      <button
        class="btn btn-primary btn-sm"
        @click="$emit('accept')"
        :disabled="disabled"
      >
        Accept
      </button>
      <button
        class="btn btn-secondary btn-sm"
        @click="$emit('reject')"
        :disabled="disabled"
      >
        Reject
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  disabled?: boolean
}>()

defineEmits<{
  (e: "accept"): void
  (e: "reject"): void
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
