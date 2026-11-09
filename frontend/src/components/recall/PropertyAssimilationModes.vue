<template>
  <div
    class="grid grid-cols-[minmax(0,auto)_minmax(max-content,1fr)] items-center gap-x-2 gap-y-2"
    :class="{ 'list-assimilation-modes': isList }"
  >
    <div
      v-for="row in rows"
      :key="row.key"
      class="col-span-2 grid min-h-8 grid-cols-subgrid items-center"
      data-test="assimilation-mode-row-UNDERSTANDING"
      :data-property-value="row.propertyValue"
    >
      <span
        class="text-right text-sm font-medium"
        data-test="mode-label-UNDERSTANDING"
      >
        <RichFrontmatterListPropertyValue
          v-if="row.propertyValue !== undefined"
          :value="listPropertyValue([row.propertyValue])"
          :property-key="propertyKey"
          :wiki-links="wikiLinks"
          :last-saved-markdown="lastSavedMarkdown"
          @dead-wiki-link-click="emit('deadWikiLinkClick', $event)"
        />
        <template v-else>Understanding</template>
      </span>
      <div
        class="flex min-h-8 min-w-0 items-center gap-2"
        data-test="assimilation-action-UNDERSTANDING"
      >
        <AssimilationModeAction
          mode="UNDERSTANDING"
          :tracker="row.tracker"
          :loading="loading && showLearningControls"
        >
          <div :class="row.showSkipAffordance ? 'daisy-join' : undefined">
            <button
              v-if="row.assimilable"
              type="button"
              :class="[
                'daisy-btn daisy-btn-primary daisy-btn-sm',
                row.showSkipAffordance ? 'daisy-join-item' : '',
              ]"
              data-test="assimilate-UNDERSTANDING"
              :aria-label="row.assimilateLabel"
              :disabled="disabled"
              @click="
                $emit('assimilate', { propertyKey, propertyValue: row.propertyValue })
              "
            >
              Assimilate
            </button>
            <template v-if="row.showSkipAffordance">
              <button
                v-if="skippedFromAssimilationSequence"
                type="button"
                class="daisy-btn daisy-btn-ghost daisy-join-item daisy-btn-sm"
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
                class="daisy-btn daisy-btn-ghost daisy-join-item daisy-btn-sm"
                data-test="skip"
                aria-label="Skip Understanding"
                :disabled="disabled"
                @click="$emit('skip')"
              >
                Skip
              </button>
            </template>
          </div>
        </AssimilationModeAction>
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
import AssimilationModeAction from "./AssimilationModeAction.vue"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"
import { noteLevelTrackerOfType } from "./assimilationMemoryTrackers"

const props = defineProps<{
  trackers?: MemoryTracker[]
  propertyKey: string
  propertyValues?: readonly string[]
  authoredPropertyValues?: readonly string[]
  showLearningControls?: boolean
  loading?: boolean
  disabled?: boolean
  skippedFromAssimilationSequence?: boolean
  wikiLinks?: WikiLink[]
  lastSavedMarkdown?: string
}>()

const emit = defineEmits<{
  assimilate: [request: AssimilateEvent]
  skip: []
  returnToSequence: []
  deadWikiLinkClick: [payload: DeadWikiLinkPayload]
}>()

const isList = computed(
  () =>
    props.authoredPropertyValues !== undefined ||
    props.propertyValues !== undefined
)

function unitRow(
  propertyValue?: string,
  learning: { eligible: boolean; firstOccurrence: boolean } = {
    eligible: true,
    firstOccurrence: true,
  }
) {
  return {
    key: propertyValue ?? "",
    propertyValue,
    assimilable:
      props.showLearningControls &&
      learning.eligible &&
      learning.firstOccurrence,
    assimilateLabel:
      propertyValue === undefined
        ? "Assimilate as Understanding"
        : `Assimilate ${propertyValue}`,
    tracker: learning.eligible
      ? noteLevelTrackerOfType(
          props.trackers,
          "UNDERSTANDING",
          props.propertyKey,
          propertyValue
        )
      : undefined,
    showSkipAffordance:
      propertyValue === undefined && props.showLearningControls,
  }
}

const rows = computed(() => {
  const displayValues = props.authoredPropertyValues ?? props.propertyValues
  if (!displayValues) return [unitRow()]
  const eligibleValues = new Set(props.propertyValues ?? displayValues)
  return displayValues.map((value, index) => ({
    ...unitRow(value, {
      eligible: eligibleValues.has(value),
      firstOccurrence: displayValues.indexOf(value) === index,
    }),
    key: `${index}:${value}`,
  }))
})
</script>

<style scoped lang="scss" src="./PropertyAssimilationModes.scss"></style>
