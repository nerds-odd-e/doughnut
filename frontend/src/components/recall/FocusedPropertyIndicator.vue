<template>
  <p
    v-if="propertyKey"
    class="focused-property text-sm font-medium mb-2"
    data-testid="focused-property-indicator"
  >
    Focused property: {{ propertyKey
    }}<template v-if="propertyValue">: {{ propertyValue }}</template>
  </p>
</template>

<script setup lang="ts">
defineProps<{
  propertyKey?: string
  propertyValue?: string
}>()
</script>
