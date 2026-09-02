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
    <AssimilationModes
      v-if="noteId && !isNoteLevelPropertyKey(propertyKey)"
      size="sm"
      :allowed-modes="allowedModes"
      :trackers="noteRecallInfo?.memoryTrackers"
      :property-key="propertyKey"
      :disabled="assimilatingPropertyKey === propertyKey"
      :skipped-from-assimilation-sequence="
        isSkippedFromAssimilationSequence(noteRecallInfo, propertyKey)
      "
      @assimilate="assimilate"
      @skip="skip({ propertyKey })"
      @return-to-sequence="returnToSequence({ propertyKey })"
    />
  </div>
</template>

<script setup lang="ts">
import { Minus } from "@lucide/vue"
import { toRef } from "vue"
import AssimilationModes from "@/components/recall/AssimilationModes.vue"
import type { MemoryTrackerType } from "@/components/recall/assimilationMemoryTrackers"
import { useInjectedMemoryTrackerActions } from "@/composables/useMemoryTrackerActions"
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
  returnToSequence,
} = useInjectedMemoryTrackerActions(toRef(() => props.noteId ?? 0))

const allowedModes: MemoryTrackerType[] = ["UNDERSTANDING"]
</script>
