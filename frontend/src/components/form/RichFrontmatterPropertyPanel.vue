<template>
  <div
    class="pl-8 flex flex-wrap items-center gap-2 gap-y-1"
    data-testid="rich-note-property-panel"
  >
    <button
      type="button"
      class="daisy-btn daisy-btn-ghost daisy-btn-sm square shrink-0"
      :aria-label="`Remove note property ${propertyKey}`"
      data-testid="rich-note-property-row-remove"
      @click="emit('remove')"
    >
      <Minus class="h-4 w-4" aria-hidden="true" />
    </button>
    <AssimilationButtons
      v-if="noteId && !isNoteLevelPropertyKey(propertyKey)"
      size="sm"
      :disabled="assimilatingPropertyKey === propertyKey"
      :assimilate-disabled="
        assimilateDisabledForProperty(noteRecallInfo, propertyKey)
      "
      :skipped-for-recall="isSkippedForRecall(noteRecallInfo, propertyKey)"
      :skipped-from-assimilation-sequence="
        isSkippedFromAssimilationSequence(noteRecallInfo, propertyKey)
      "
      :show-remove-from-recall="showRemoveFromRecall(noteRecallInfo, propertyKey)"
      @assimilate="assimilate({ propertyKey })"
      @skip="skip({ propertyKey })"
      @revive="revive({ propertyKey })"
      @return-to-sequence="returnToSequence({ propertyKey })"
      @remove-from-recall="removeFromRecall({ propertyKey })"
    />
  </div>
</template>

<script setup lang="ts">
import { Minus } from "@lucide/vue"
import { toRef } from "vue"
import AssimilationButtons from "@/components/recall/AssimilationButtons.vue"
import {
  assimilateDisabledForProperty,
  showRemoveFromRecall,
} from "@/components/recall/assimilationMemoryTrackers"
import { useInjectedMemoryTrackerActions } from "@/composables/useMemoryTrackerActions"
import { isSkippedForRecall } from "@/composables/useReviveMemoryTracker"
import { isSkippedFromAssimilationSequence } from "@/composables/useAssimilationSequenceSkip"
import { isNoteLevelPropertyKey } from "@/utils/noteContentPropertyKeys"

const props = defineProps<{
  propertyKey: string
  noteId?: number
}>()

const emit = defineEmits<{
  remove: []
}>()

const {
  noteRecallInfo,
  assimilatingPropertyKey,
  assimilate,
  skip,
  revive,
  returnToSequence,
  removeFromRecall,
} = useInjectedMemoryTrackerActions(toRef(() => props.noteId ?? 0))
</script>
