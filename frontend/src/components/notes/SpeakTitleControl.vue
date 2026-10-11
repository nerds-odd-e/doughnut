<template>
  <button
    type="button"
    :class="buttonClass"
    :title="name"
    :aria-label="name"
    :aria-pressed="phase === 'listening' || undefined"
    :disabled="phase === 'converting'"
    @click="onControlClick"
  >
    <LoaderCircle
      v-if="phase === 'converting'"
      class="w-6 h-6 animate-spin"
      aria-hidden="true"
    />
    <Mic v-else class="w-6 h-6" aria-hidden="true" />
  </button>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { LoaderCircle, Mic } from "@lucide/vue"
import type { TitleVoiceInput } from "@/composables/useTitleVoiceInput"
import { fieldJoinAppendToggleButtonClass } from "@/utils/fieldJoinAppendButtonClass"
import { toolbarToggleBtnClass } from "./widgets/noteToolbarButtonClasses"

const { joinsField, voiceInput } = defineProps<{
  voiceInput: TitleVoiceInput
  /** Styled as a button appended to a field control in a `daisy-join`. */
  joinsField?: boolean
}>()

const { phase, onControlClick } = voiceInput

const name = computed(() =>
  phase.value === "listening" ? "Stop speaking the title" : "Speak the title"
)

const buttonClass = computed(() => {
  const listening = phase.value === "listening"
  return joinsField
    ? `${fieldJoinAppendToggleButtonClass(listening)} rounded-none`
    : toolbarToggleBtnClass(listening)
})
</script>
