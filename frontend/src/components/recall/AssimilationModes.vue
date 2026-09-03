<template>
  <div class="flex flex-col gap-2" data-testid="assimilation-modes">
    <div
      v-for="row in rows"
      :key="row.mode"
      class="flex flex-wrap items-center gap-2"
      :data-test="`assimilation-mode-row-${row.mode}`"
    >
      <span class="text-sm font-medium" :data-test="`mode-label-${row.mode}`">
        {{ row.label }}
      </span>
      <router-link
        v-if="row.tracker"
        :to="row.trackerLocation!"
        class="daisy-link daisy-link-primary"
        :data-test="`assimilation-status-${row.mode}`"
        :title="row.statusTitle"
      >
        {{ row.statusText }}
      </router-link>
      <div
        v-else
        :class="row.showSkipAffordance ? 'daisy-join' : undefined"
      >
        <input
          type="submit"
          value="Assimilate"
          :class="[
            'daisy-btn daisy-btn-primary',
            row.showSkipAffordance ? 'daisy-join-item' : '',
            sizeClass,
          ]"
          :data-test="`assimilate-${row.mode}`"
          :disabled="disabled"
          @click="$emit('assimilate', assimilatePayloadFor(row.mode))"
        />
        <template v-if="row.showSkipAffordance">
          <input
            v-if="skippedFromAssimilationSequence"
            type="submit"
            value="Return to sequence"
            :class="['daisy-btn daisy-btn-secondary daisy-join-item', sizeClass]"
            data-test="return-to-sequence"
            :disabled="disabled"
            @click="$emit('returnToSequence')"
          />
          <input
            v-else
            type="submit"
            value="Skip"
            :class="['daisy-btn daisy-btn-secondary daisy-join-item', sizeClass]"
            data-test="skip"
            :disabled="disabled"
            @click="$emit('skip')"
          />
        </template>
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
    disabled?: boolean
    skippedFromAssimilationSequence?: boolean
    size?: "default" | "sm"
  }>(),
  {
    trackers: () => [],
    propertyKey: undefined,
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

function formatNextRecallAt(nextRecallAt: string): string {
  return new Date(nextRecallAt).toLocaleDateString(undefined, {
    day: "numeric",
    month: "short",
  })
}

function assimilatePayloadFor(mode: MemoryTrackerType): AssimilateEvent {
  return {
    propertyKey: props.propertyKey,
    assimilateAsCommissioned: mode === "COMMISSIONED" ? true : undefined,
    assimilateAsSpelling: mode === "SPELLING" ? true : undefined,
  }
}

const rows = computed(() =>
  props.allowedModes.map((mode) => {
    const tracker = noteLevelTrackerOfType(
      props.trackers,
      mode,
      props.propertyKey
    )
    return {
      mode,
      label: modeLabels[mode],
      tracker,
      trackerLocation: tracker
        ? {
            name: "memoryTrackerShow",
            params: { memoryTrackerId: tracker.id },
          }
        : undefined,
      statusText: tracker
        ? `In recall · next ${formatNextRecallAt(tracker.nextRecallAt)}`
        : undefined,
      statusTitle: tracker
        ? `Recalled ${tracker.recallCount ?? 0} times`
        : undefined,
      showSkipAffordance: mode === "UNDERSTANDING",
    }
  })
)
</script>
