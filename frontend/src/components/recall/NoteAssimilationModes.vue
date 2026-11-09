<template>
  <section
    aria-label="Assimilate this note"
    data-testid="note-assimilation-modes"
    :class="
      readFirst
        ? 'border-base-300 bg-base-100 border-t px-4 pt-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]'
        : 'rounded-box border-base-300 mb-4 border p-4'
    "
  >
    <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
      <h3 class="!m-0 !text-base font-semibold">Assimilate this note</h3>
      <AutoCollapseDropdown
        v-if="readFirst"
        v-slot="{ closeDropdown }"
        ref="optionalModesDisclosure"
        class="daisy-dropdown daisy-dropdown-end daisy-dropdown-top"
        @keydown.esc="optionalModesDisclosure?.closeDropdown()"
        @toggle="keepFocusAtTrigger"
      >
        <summary
          class="daisy-btn daisy-btn-sm list-none"
          data-test="optional-modes"
        >
          Optional modes
          <ChevronUp class="size-4" aria-hidden="true" />
        </summary>
        <div
          class="daisy-dropdown-content bg-base-100 rounded-box mb-2 flex w-[min(26rem,calc(100vw-4rem))] flex-col gap-3 p-4 shadow"
        >
          <h4 class="font-semibold">Optional modes</h4>
          <OptionalAssimilationModes
            :trackers="noteRecallInfo?.memoryTrackers"
            :loading="!noteInfoLoaded"
            @assimilate="closeDropdown(); emit('assimilate', $event)"
          />
        </div>
      </AutoCollapseDropdown>
    </div>
    <div :class="{ 'grid grid-cols-2 gap-6': !readFirst }">
      <div
        class="flex flex-col items-start gap-1"
        data-test="assimilation-mode-row-UNDERSTANDING"
      >
        <template v-if="!readFirst">
          <div
            class="text-base-content/60 text-xs font-semibold tracking-wide uppercase"
          >
            Default mode
          </div>
          <h4 class="text-lg font-semibold">Understanding</h4>
          <p class="text-base-content/70 mb-2 text-sm">
            Recall what this note means.
          </p>
        </template>
        <div
          v-if="understandingAdded"
          class="flex flex-col items-start gap-2"
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
        <div v-else class="flex flex-wrap items-center gap-2">
          <AssimilationModeAction
            mode="UNDERSTANDING"
            :tracker="understandingTracker"
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
      <div v-if="!readFirst" class="flex flex-col gap-3">
        <div
          class="text-base-content/60 text-xs font-semibold tracking-wide uppercase"
        >
          Optional modes
        </div>
        <OptionalAssimilationModes
          :trackers="noteRecallInfo?.memoryTrackers"
          :loading="!noteInfoLoaded"
          @assimilate="emit('assimilate', $event)"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { Note } from "@generated/donut-backend-api"
import { ArrowRight, Check, ChevronUp } from "@lucide/vue"
import { computed, ref, toRef } from "vue"
import AutoCollapseDropdown from "@/components/commons/AutoCollapseDropdown.vue"
import AssimilationModeAction from "./AssimilationModeAction.vue"
import OptionalAssimilationModes from "./OptionalAssimilationModes.vue"
import type { AssimilateEvent } from "@/composables/useAssimilateUnit"
import { isSkippedFromAssimilationSequence } from "@/composables/useAssimilationSequenceSkip"
import { useInjectedMemoryTrackerActions } from "@/composables/useMemoryTrackerActions"
import { noteLevelTrackerOfType } from "./assimilationMemoryTrackers"

const { note, noteInfoLoaded } = defineProps<{
  note: Note
  noteInfoLoaded: boolean
  /** A bottom action bar under the note instead of the panel after its title. */
  readFirst: boolean
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

const understandingTracker = computed(() =>
  noteLevelTrackerOfType(noteRecallInfo.value?.memoryTrackers, "UNDERSTANDING")
)

const skipped = computed(() =>
  isSkippedFromAssimilationSequence(noteRecallInfo.value)
)

const optionalModesDisclosure = ref<InstanceType<
  typeof AutoCollapseDropdown
> | null>(null)

/** Closing must not strand focus inside the hidden popover or on the page. */
const keepFocusAtTrigger = (event: Event) => {
  const disclosure = event.target as HTMLDetailsElement
  const focused = document.activeElement
  if (
    !disclosure.open &&
    (focused === document.body || disclosure.contains(focused))
  ) {
    disclosure.querySelector("summary")!.focus()
  }
}
</script>
