<template>
  <button
    type="button"
    :class="buttonClass"
    :title="name"
    :aria-label="name"
    :aria-pressed="phase === 'listening' || undefined"
    :disabled="phase === 'starting' || phase === 'converting'"
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

const { voiceInput } = defineProps<{
  voiceInput: TitleVoiceInput
}>()

const { phase, onControlClick } = voiceInput

const name = computed(() =>
  phase.value === "listening" ? "Stop speaking the title" : "Speak the title"
)

const buttonClass = computed(
  () =>
    `${fieldJoinAppendToggleButtonClass(phase.value === "listening")} rounded-none`
)
</script>
