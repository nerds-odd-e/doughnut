<template>
  <button
    :class="btnClass || 'daisy-btn daisy-btn-secondary daisy-btn-circle'"
    @click="copyToClipboard"
    :disabled="disabled || !text"
    :aria-label="ariaLabel || 'Copy to clipboard'"
    :data-testid="testId"
  >
    <SvgClipboard v-if="!copied" />
    <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" viewBox="0 0 24 24">
      <path stroke="currentColor" stroke-width="2" d="M5 13l4 4L19 7"/>
    </svg>
  </button>
</template>

<script setup lang="ts">
import { ref } from "vue"
import SvgClipboard from "../svgs/SvgClipboard.vue"

const props = withDefaults(
  defineProps<{
    text: string
    disabled?: boolean
    btnClass?: string
    timeout?: number
    ariaLabel?: string
    testId?: string
  }>(),
  {
    timeout: 1200,
    disabled: false,
  }
)

const emit = defineEmits<{
  (e: "copied"): void
}>()

const copied = ref(false)

async function copyToClipboard() {
  if (!props.text) return
  await navigator.clipboard.writeText(props.text)
  copied.value = true
  emit("copied")
  setTimeout(() => {
    copied.value = false
  }, props.timeout)
}
</script>

