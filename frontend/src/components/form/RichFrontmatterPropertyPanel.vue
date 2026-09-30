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
    <template v-if="noteId">
      <button
        type="button"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm shrink-0"
        :aria-label="`Reify note property ${propertyKey}`"
        :aria-describedby="reifiable ? undefined : reifyReasonId"
        :disabled="!reifiable"
        data-testid="rich-note-property-row-reify"
        @click="noteStore.reifyProperty(router, noteId, propertyKey)"
      >
        Reify
      </button>
      <span
        v-if="!reifiable"
        :id="reifyReasonId"
        class="text-xs text-base-content/70"
      >
        Only a property whose value is a link to a note can be reified
      </span>
    </template>
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
import { toRef, useId } from "vue"
import { useRouter } from "vue-router"
import AssimilationModes from "@/components/recall/AssimilationModes.vue"
import type { MemoryTrackerType } from "@/components/recall/assimilationMemoryTrackers"
import { useInjectedMemoryTrackerActions } from "@/composables/useMemoryTrackerActions"
import { isSkippedFromAssimilationSequence } from "@/composables/useAssimilationSequenceSkip"
import { isNoteLevelPropertyKey } from "@/utils/noteContentPropertyKeys"
import { useNoteStore } from "@/store/noteStore"

const props = defineProps<{
  propertyKey: string
  noteId?: number
  reifiable: boolean
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

const router = useRouter()
const noteStore = useNoteStore()

const reifyReasonId = useId()

const allowedModes: MemoryTrackerType[] = ["UNDERSTANDING"]
</script>
