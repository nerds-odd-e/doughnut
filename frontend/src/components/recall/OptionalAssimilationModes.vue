<template>
  <div
    v-for="option in optionalModes"
    :key="option.mode"
    class="flex flex-wrap items-center justify-between gap-2"
    :data-test="`assimilation-mode-row-${option.mode}`"
  >
    <div class="flex min-w-0 flex-[1_1_11rem] items-start gap-2">
      <component
        :is="option.icon"
        class="mt-0.5 size-5 shrink-0"
        aria-hidden="true"
      />
      <div>
        <strong>{{ option.label }}</strong>
        <p class="text-base-content/70 text-sm">
          {{ option.description }}
        </p>
      </div>
    </div>
    <div class="flex items-center gap-2">
      <AssimilationModeAction
        :mode="option.mode"
        :tracker="noteLevelTrackerOfType(trackers, option.mode)"
        :loading="loading"
      >
        <button
          type="button"
          class="daisy-btn daisy-btn-outline"
          :data-test="`assimilate-${option.mode}`"
          @click="emit('assimilate', option.request)"
        >
          {{ option.action }}
        </button>
      </AssimilationModeAction>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { MemoryTracker } from "@generated/donut-backend-api"
import { GraduationCap, SpellCheck } from "@lucide/vue"
import AssimilationModeAction from "./AssimilationModeAction.vue"
import type { AssimilateEvent } from "@/composables/useAssimilateUnit"
import { noteLevelTrackerOfType } from "./assimilationMemoryTrackers"

defineProps<{
  trackers: MemoryTracker[] | undefined
  loading: boolean
}>()

const emit = defineEmits<{
  (e: "assimilate", request: AssimilateEvent): void
}>()

const optionalModes = [
  {
    mode: "SPELLING",
    icon: SpellCheck,
    label: "Spelling",
    description: "Recall the title or an alias from memory.",
    action: "Verify spelling",
    request: { assimilateAsSpelling: true },
  },
  {
    mode: "COMMISSIONED",
    icon: GraduationCap,
    label: "Commissioned",
    description: "Review in future learning sessions with a human or AI tutor.",
    action: "Add to sessions",
    request: { assimilateAsCommissioned: true },
  },
] as const
</script>
