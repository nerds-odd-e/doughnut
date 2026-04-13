<template>
  <div
    ref="rootRef"
    class="daisy-pointer-events-auto daisy-mx-auto daisy-max-w-3xl daisy-rounded-lg daisy-bg-base-200/95 daisy-border daisy-border-base-300 daisy-shadow-lg daisy-px-3 daisy-py-2 daisy-flex daisy-flex-wrap daisy-items-center daisy-gap-2 daisy-relative"
  >
    <span
      v-if="showCaret"
      aria-hidden="true"
      class="daisy-absolute daisy-left-[30%] -daisy-top-[9px] -daisy-translate-x-1/2 daisy-w-4 daisy-h-4 daisy-bg-base-200/95 daisy-border-l daisy-border-t daisy-border-base-300 -daisy-rotate-45"
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
