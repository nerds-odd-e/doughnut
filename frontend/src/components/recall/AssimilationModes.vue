<template>
  <div
    class="grid grid-cols-[max-content_minmax(0,1fr)] items-center gap-x-2 gap-y-2"
    data-testid="assimilation-modes"
  >
    <div
      v-for="row in rows"
      :key="row.key"
      class="col-span-2 grid grid-cols-subgrid items-center"
      :class="rowHeightClass"
      :data-test="`assimilation-mode-row-${row.mode}`"
      :data-property-value="row.propertyValue"
    >
      <span
        class="text-right text-sm font-medium"
        :data-test="`mode-label-${row.mode}`"
      >
        {{ row.label }}
      </span>
      <div
        class="flex min-w-0 items-center gap-2"
        :class="rowHeightClass"
        :data-test="`assimilation-action-${row.mode}`"
      >
        <template v-if="row.tracker">
          <router-link
            :to="row.trackerLocation!"
            class="daisy-link daisy-link-primary shrink-0 text-sm font-medium focus-visible:outline-2 focus-visible:outline-offset-2"
            :data-test="`assimilation-status-${row.mode}`"
            :title="row.statusTitle"
          >
            View tracker
          </router-link>
          <span class="text-base-content/60 shrink-0 text-xs">
            Next {{ row.nextRecallAtText }}
          </span>
        </template>
        <div
          v-else
          :class="row.showSkipAffordance ? 'daisy-join' : undefined"
        >
          <button
            v-if="row.assimilable"
            type="button"
            :class="[
              'daisy-btn daisy-btn-primary',
              row.showSkipAffordance ? 'daisy-join-item' : '',
              sizeClass,
            ]"
            :data-test="`assimilate-${row.mode}`"
            :aria-label="row.assimilateLabel"
            :disabled="disabled"
            @click="
              $emit('assimilate', assimilatePayloadFor(row.mode, row.propertyValue))
            "
          >
            Assimilate
          </button>
          <template v-if="row.showSkipAffordance">
            <button
              v-if="skippedFromAssimilationSequence"
              type="button"
              :class="['daisy-btn daisy-btn-ghost daisy-join-item', sizeClass]"
              data-test="return-to-sequence"
              aria-label="Return Understanding to sequence"
              :disabled="disabled"
              @click="$emit('returnToSequence')"
            >
              Return to sequence
            </button>
            <button
              v-else
              type="button"
              :class="['daisy-btn daisy-btn-ghost daisy-join-item', sizeClass]"
              data-test="skip"
              aria-label="Skip Understanding"
              :disabled="disabled"
              @click="$emit('skip')"
            >
              Skip
            </button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { MemoryTracker } from "@generated/donut-backend-api"
import { computed } from "vue"
import type { AssimilateEvent } from "@/composables/useAssimilateUnit"
import {
  noteLevelTrackerOfType,
  type MemoryTrackerType,
} from "./assimilationMemoryTrackers"

const props = withDefaults(
  defineProps<{
    allowedModes: MemoryTrackerType[]
    trackers?: MemoryTracker[]
    propertyKey?: string
    propertyValues?: readonly string[]
    disabled?: boolean
    skippedFromAssimilationSequence?: boolean
    size?: "default" | "sm"
  }>(),
  {
    trackers: () => [],
    propertyKey: undefined,
    propertyValues: undefined,
    disabled: false,
    skippedFromAssimilationSequence: false,
    size: "default",
  }
)

defineEmits<{
  assimilate: [request: AssimilateEvent]
  skip: []
  returnToSequence: []
}>()

const modeLabels: Record<MemoryTrackerType, string> = {
  COMMISSIONED: "Commissioned",
  SPELLING: "Spelling",
  UNDERSTANDING: "Understanding",
}

const sizeClass = computed(() => (props.size === "sm" ? "daisy-btn-sm" : ""))
const rowHeightClass = computed(() =>
  props.size === "sm" ? "min-h-8" : "min-h-12"
)

function formatNextRecallAt(nextRecallAt: string): string {
  return new Date(nextRecallAt).toLocaleDateString(undefined, {
    day: "numeric",
    month: "short",
  })
}

function assimilatePayloadFor(
  mode: MemoryTrackerType,
  propertyValue?: string
): AssimilateEvent {
  return {
    propertyKey: props.propertyKey,
    propertyValue,
    assimilateAsCommissioned: mode === "COMMISSIONED" ? true : undefined,
    assimilateAsSpelling: mode === "SPELLING" ? true : undefined,
  }
}

function unitRow(mode: MemoryTrackerType, propertyValue?: string) {
  const tracker = noteLevelTrackerOfType(
    props.trackers,
    mode,
    props.propertyKey,
    propertyValue
  )
  return {
    key: `${mode}:${propertyValue ?? ""}`,
    mode,
    propertyValue,
    label: propertyValue ?? modeLabels[mode],
    assimilable: true,
    assimilateLabel:
      propertyValue === undefined
        ? `Assimilate as ${modeLabels[mode]}`
        : `Assimilate ${propertyValue}`,
    tracker,
    trackerLocation: tracker
      ? {
          name: "memoryTrackerShow",
          params: { memoryTrackerId: tracker.id },
        }
      : undefined,
    nextRecallAtText: tracker
      ? formatNextRecallAt(tracker.nextRecallAt)
      : undefined,
    statusTitle: tracker
      ? `Recalled ${tracker.recallCount ?? 0} times`
      : undefined,
    showSkipAffordance: mode === "UNDERSTANDING" && !props.propertyValues,
  }
}

/** A list property is skipped as a whole, so its values share one Skip row. */
function keySkipRow() {
  return {
    ...unitRow("UNDERSTANDING"),
    key: "skip",
    label: "",
    assimilable: false,
    tracker: undefined,
    showSkipAffordance: true,
  }
}

const rows = computed(() =>
  props.allowedModes.flatMap((mode) => {
    if (!props.propertyValues) return [unitRow(mode)]
    const valueRows = props.propertyValues.map((value) => unitRow(mode, value))
    return mode === "UNDERSTANDING" ? [...valueRows, keySkipRow()] : valueRows
  })
)
</script>
