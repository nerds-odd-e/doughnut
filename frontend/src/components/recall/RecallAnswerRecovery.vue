<template>
  <div class="daisy-alert daisy-alert-error" data-testid="answer-recovery">
    <span>We couldn't confirm your answer. Retry to recover this attempt.</span>
    <button
      type="button"
      class="daisy-btn daisy-btn-sm"
      data-testid="retry-answer"
      @click="$emit('retry')"
    >
      Retry
    </button>
  </div>
</template>

<script setup lang="ts">
defineEmits<{ (e: "retry"): void }>()
</script>
