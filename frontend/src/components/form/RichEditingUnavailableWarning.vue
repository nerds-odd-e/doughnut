<template>
  <div
    role="alert"
    aria-live="polite"
    data-testid="rich-note-unavailable-warning"
    class="daisy-alert daisy-alert-warning mb-3 text-sm"
  >
    <div class="flex min-w-0 flex-col items-start gap-2">
      <template v-if="frontmatterProblem !== undefined">
        <span>{{ frontmatterProblem }}</span>
        <span class="text-xs opacity-90">
          Switch to Markdown mode to fix the frontmatter.
        </span>
      </template>
      <template v-else>
        <span>This note has content the rich editor cannot keep.</span>
        <span class="text-xs opacity-90">
          Converting keeps the text and removes formatting the rich editor
          cannot keep.
        </span>
        <button
          type="button"
          class="daisy-btn daisy-btn-sm pointer-coarse:min-h-11"
          @click="$emit('convert')"
        >
          Convert for the rich editor
        </button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
/** Without a frontmatter problem, the body is what the rich editor cannot keep. */
defineProps<{ frontmatterProblem?: string }>()
defineEmits<{ (e: "convert"): void }>()
</script>
