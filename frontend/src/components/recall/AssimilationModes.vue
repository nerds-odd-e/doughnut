<template>
  <div
    class="grid grid-cols-[minmax(0,auto)_minmax(max-content,1fr)] items-center gap-x-2 gap-y-2"
    :class="{ 'list-assimilation-modes': isList }"
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
        <RichFrontmatterListPropertyValue
          v-if="row.propertyValue !== undefined"
          :value="listPropertyValue([row.propertyValue])"
          :property-key="propertyKey"
          :wiki-links="wikiLinks"
          :last-saved-markdown="lastSavedMarkdown"
          @dead-wiki-link-click="emit('deadWikiLinkClick', $event)"
        />
        <template v-else>{{ row.label }}</template>
      </span>
      <div
        class="flex min-w-0 items-center gap-2"
        :class="rowHeightClass"
        :data-test="`assimilation-action-${row.mode}`"
      >
        <span
          v-if="loading && showLearningControls"
          class="daisy-loading daisy-loading-spinner daisy-loading-xs"
          role="status"
          aria-label="Loading tracker information"
          data-app-busy="true"
        />
        <template v-else-if="row.tracker">
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
import type { MemoryTracker, WikiLink } from "@generated/donut-backend-api"
import { computed } from "vue"
import RichFrontmatterListPropertyValue from "@/components/form/RichFrontmatterListPropertyValue.vue"
import type { AssimilateEvent } from "@/composables/useAssimilateUnit"
import { listPropertyValue } from "@/utils/noteProperties"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"
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
    authoredPropertyValues?: readonly string[]
    showLearningControls?: boolean
    loading?: boolean
    disabled?: boolean
    skippedFromAssimilationSequence?: boolean
    size?: "default" | "sm"
    wikiLinks?: WikiLink[]
    lastSavedMarkdown?: string
  }>(),
  {
    trackers: () => [],
    showLearningControls: true,
    loading: false,
    disabled: false,
    skippedFromAssimilationSequence: false,
    size: "default",
  }
)

const emit = defineEmits<{
  assimilate: [request: AssimilateEvent]
  skip: []
  returnToSequence: []
  deadWikiLinkClick: [payload: DeadWikiLinkPayload]
}>()

const modeLabels: Record<MemoryTrackerType, string> = {
  COMMISSIONED: "Commissioned",
  SPELLING: "Spelling",
  UNDERSTANDING: "Understanding",
}

const sizeClass = computed(() => (props.size === "sm" ? "daisy-btn-sm" : ""))
const isList = computed(
  () =>
    props.authoredPropertyValues !== undefined ||
    props.propertyValues !== undefined
)
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

function unitRow(
  mode: MemoryTrackerType,
  propertyValue?: string,
  learning: { eligible: boolean; firstOccurrence: boolean } = {
    eligible: true,
    firstOccurrence: true,
  }
) {
  const tracker = learning.eligible
    ? noteLevelTrackerOfType(
        props.trackers,
        mode,
        props.propertyKey,
        propertyValue
      )
    : undefined
  return {
    key: `${mode}:${propertyValue ?? ""}`,
    mode,
    propertyValue,
    label: propertyValue ?? modeLabels[mode],
    assimilable:
      props.showLearningControls &&
      learning.eligible &&
      learning.firstOccurrence,
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
    showSkipAffordance:
      mode === "UNDERSTANDING" &&
      propertyValue === undefined &&
      props.showLearningControls,
  }
}

const rows = computed(() => {
  const displayValues = props.authoredPropertyValues ?? props.propertyValues
  if (!displayValues) return props.allowedModes.map((mode) => unitRow(mode))
  const eligibleValues = new Set(props.propertyValues ?? displayValues)
  return props.allowedModes.flatMap((mode) =>
    displayValues.map((value, index) => ({
      ...unitRow(mode, value, {
        eligible: eligibleValues.has(value),
        firstOccurrence: displayValues.indexOf(value) === index,
      }),
      key: `${mode}:${index}:${value}`,
    }))
  )
})
</script>

<style scoped lang="scss" src="./AssimilationModes.scss"></style>
