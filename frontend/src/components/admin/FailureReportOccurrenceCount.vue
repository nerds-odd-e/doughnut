<template>
  <span class="inline-flex items-center gap-1">
    <Repeat class="shrink-0" :size="size" />
    <slot />
    <span data-testid="failure-report-occurrence-count">{{ count }}</span>
  </span>
</template>

<script setup lang="ts">
import { Repeat } from "@lucide/vue"

withDefaults(
  defineProps<{
    count: number
    size?: number
  }>(),
  { size: 16 }
)
</script>
