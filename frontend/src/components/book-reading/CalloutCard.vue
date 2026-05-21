<template>
  <div
    ref="rootRef"
    class="pointer-events-auto mx-auto max-w-3xl rounded-lg bg-base-200/95 border border-base-300 shadow-lg px-3 py-2 flex flex-wrap items-center gap-2 relative"
  >
    <span
      v-if="showCaret"
      aria-hidden="true"
      class="absolute left-[30%] -top-[9px] -translate-x-1/2 w-4 h-4 bg-base-200/95 border-l border-t border-base-300 -rotate-45"
    />
    <slot />
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"

withDefaults(defineProps<{ showCaret?: boolean }>(), { showCaret: false })

const rootRef = ref<HTMLElement | null>(null)
defineExpose({ el: rootRef })
</script>
