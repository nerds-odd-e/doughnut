<template>
  <section
    aria-label="Assimilate this note"
    data-testid="note-assimilation-modes"
    class="@container rounded-box border-base-300 mb-4 border p-4"
  >
    <h3 class="mb-3 text-base font-semibold">Assimilate this note</h3>
    <div class="grid gap-6 @lg:grid-cols-2">
      <div
        class="flex flex-col items-start gap-1"
        data-test="assimilation-mode-row-UNDERSTANDING"
      >
        <div
          class="text-base-content/60 text-xs font-semibold tracking-wide uppercase"
        >
          Default mode
        </div>
        <h4 class="text-lg font-semibold">Understanding</h4>
        <p class="text-base-content/70 text-sm">Recall what this note means.</p>
        <div
          v-if="understandingAdded"
          class="mt-2 flex flex-col items-start gap-2"
          data-test="understanding-result"
        >
          <p role="status" class="text-success flex items-center gap-2 font-medium">
            <Check class="size-5" aria-hidden="true" />
            Understanding added to recall
          </p>
          <button
            type="button"
            class="daisy-btn daisy-btn-primary"
            data-test="next-item"
            @click="emit('next')"
          >
            Next item
            <ArrowRight class="size-4" aria-hidden="true" />
          </button>
        </div>
        <div v-else class="mt-2 flex flex-wrap items-center gap-2">
          <AssimilationModeAction
            mode="UNDERSTANDING"
            :tracker="trackerOf('UNDERSTANDING')"
            :loading="!noteInfoLoaded"
          >
            <button
              type="button"
              class="daisy-btn daisy-btn-primary"
              data-test="assimilate-UNDERSTANDING"
              @click="emit('assimilate', {})"
            >
              Assimilate understanding
              <ArrowRight class="size-4" aria-hidden="true" />
            </button>
            <button
              v-if="skipped"
              type="button"
              class="daisy-btn daisy-btn-ghost"
              data-test="return-to-sequence"
              @click="emit('returnToSequence', {})"
            >
              Return to sequence
            </button>
            <button
              v-else
              type="button"
              class="daisy-btn daisy-btn-ghost"
              data-test="skip"
              @click="emit('skip', {})"
            >
              Skip from sequence…
            </button>
          </AssimilationModeAction>
        </div>
      </div>
      <div class="flex flex-col gap-3">
        <div
          class="text-base-content/60 text-xs font-semibold tracking-wide uppercase"
        >
          Optional modes
        </div>
        <div
          v-for="option in optionalModes"
          :key="option.mode"
          class="flex flex-wrap items-center justify-between gap-2"
          :data-test="`assimilation-mode-row-${option.mode}`"
        >
          <div class="flex min-w-0 flex-1 items-start gap-2">
            <component
              :is="option.icon"
              class="mt-0.5 size-5 shrink-0"
              aria-hidden="true"
            />
            <div class="min-w-48">
              <strong>{{ option.label }}</strong>
              <p class="text-base-content/70 text-sm">
                {{ option.description }}
              </p>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <AssimilationModeAction
              :mode="option.mode"
              :tracker="trackerOf(option.mode)"
              :loading="!noteInfoLoaded"
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
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { Note } from "@generated/donut-backend-api"
import { ArrowRight, Check, GraduationCap, SpellCheck } from "@lucide/vue"
import { computed, toRef } from "vue"
import AssimilationModeAction from "./AssimilationModeAction.vue"
import type { AssimilateEvent } from "@/composables/useAssimilateUnit"
import { isSkippedFromAssimilationSequence } from "@/composables/useAssimilationSequenceSkip"
import { useInjectedMemoryTrackerActions } from "@/composables/useMemoryTrackerActions"
import {
  noteLevelTrackerOfType,
  type MemoryTrackerType,
} from "./assimilationMemoryTrackers"

const { note, noteInfoLoaded } = defineProps<{
  note: Note
  noteInfoLoaded: boolean
}>()

const emit = defineEmits<{
  (e: "assimilate", request: AssimilateEvent): void
  (e: "skip", request: { propertyKey?: string }): void
  (e: "returnToSequence", request: { propertyKey?: string }): void
  (e: "next"): void
}>()

const { noteRecallInfo, understandingAdded } = useInjectedMemoryTrackerActions(
  toRef(() => note.id)
)

const trackerOf = (mode: MemoryTrackerType) =>
  noteLevelTrackerOfType(noteRecallInfo.value?.memoryTrackers, mode)

const skipped = computed(() =>
  isSkippedFromAssimilationSequence(noteRecallInfo.value)
)

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
