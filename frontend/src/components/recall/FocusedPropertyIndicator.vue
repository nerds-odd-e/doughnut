<template>
  <p
    v-if="propertyKey"
    class="focused-property text-sm font-medium mb-2"
    data-testid="focused-property-indicator"
  >
    Focused property: {{ propertyKey }}
  </p>
</template>

<script setup lang="ts">
defineProps<{
  propertyKey?: string
}>()
</script>
