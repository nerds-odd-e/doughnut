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
        :aria-describedby="reifyRefusal ? reifyReasonId : undefined"
        :disabled="!!reifyRefusal"
        data-testid="rich-note-property-row-reify"
        @click="noteStore.reifyProperty(router, noteId, propertyKey)"
      >
        Reify
      </button>
      <span
        v-if="reifyRefusal"
        :id="reifyReasonId"
        class="text-xs text-base-content/70"
      >
        {{ reifyRefusal }}
      </span>
    </template>
    <AssimilationModes
      v-if="noteId && !isNoteLevelPropertyKey(propertyKey)"
      size="sm"
      :allowed-modes="allowedModes"
      :trackers="noteRecallInfo?.memoryTrackers"
      :property-key="propertyKey"
      :property-values="assimilableListValues(propertyValue)"
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
import { computed, toRef, useId } from "vue"
import { useRouter } from "vue-router"
import AssimilationModes from "@/components/recall/AssimilationModes.vue"
import {
  assimilableListValues,
  type MemoryTrackerType,
} from "@/components/recall/assimilationMemoryTrackers"
import { useInjectedMemoryTrackerActions } from "@/composables/useMemoryTrackerActions"
import { isSkippedFromAssimilationSequence } from "@/composables/useAssimilationSequenceSkip"
import {
  isNoteLevelPropertyKey,
  isReservedStructuralPropertyKey,
} from "@/utils/noteContentPropertyKeys"
import { isWellFormedWholeWikiLinkItem } from "@/utils/authoredLinkMarkup"
import { useNoteStore } from "@/store/noteStore"
import {
  scalarStringFromPropertyValue,
  type PropertyValue,
} from "@/utils/noteProperties"

const props = defineProps<{
  propertyKey: string
  propertyValue: PropertyValue
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

const router = useRouter()
const noteStore = useNoteStore()

const reifyReasonId = useId()

const reifyRefusal = computed(() => {
  if (isReservedStructuralPropertyKey(props.propertyKey))
    return "A structural property cannot be reified"
  const value = scalarStringFromPropertyValue(props.propertyValue) ?? ""
  if (!isWellFormedWholeWikiLinkItem(value.trim()))
    return "Only a property whose value is a link to a note can be reified"
  return undefined
})

const allowedModes: MemoryTrackerType[] = ["UNDERSTANDING"]
</script>
