<template>
  <div
    role="alert"
    aria-live="polite"
    data-testid="rich-note-unavailable-warning"
    class="daisy-alert daisy-alert-warning mb-3 text-sm"
  >
    <template v-if="frontmatterProblem !== undefined">
      <span>{{ frontmatterProblem }}</span>
      <span class="block mt-1 text-xs opacity-90">
        Switch to Markdown mode to fix the frontmatter.
      </span>
    </template>
    <template v-else>
      <span>This note has content the rich editor cannot keep.</span>
      <span class="block mt-1 text-xs opacity-90">
        Converting keeps the text and removes formatting the rich editor cannot
        keep.
      </span>
      <button
        type="button"
        class="daisy-btn daisy-btn-sm"
        @click="$emit('convert')"
      >
        Convert for the rich editor
      </button>
    </template>
  </div>
</template>

<script setup lang="ts">
/** Without a frontmatter problem, the body is what the rich editor cannot keep. */
defineProps<{ frontmatterProblem?: string }>()
defineEmits<{ (e: "convert"): void }>()
</script>
