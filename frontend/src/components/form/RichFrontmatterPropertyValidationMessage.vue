<template>
  <p
    role="alert"
    aria-live="polite"
    class="text-error text-xs mt-1"
    data-testid="rich-note-property-validation"
  >
    {{ message }}
  </p>
</template>

<script setup lang="ts">
defineProps<{
  message: string
}>()
</script>
