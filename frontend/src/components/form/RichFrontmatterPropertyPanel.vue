<template>
  <div
    class="pl-8 flex gap-2 gap-y-1"
    :class="authoredListValues ? 'items-start' : 'flex-wrap items-center'"
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
    <template v-if="noteId && !authoredListValues">
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
      v-if="showAssimilationModes"
      class="min-w-0 flex-1"
      size="sm"
      :allowed-modes="allowedModes"
      :trackers="hasSavedNote ? noteRecallInfo?.memoryTrackers : undefined"
      :property-key="propertyKey"
      :property-values="assimilableListValues(propertyValue)"
      :authored-property-values="authoredListValues"
      :show-learning-controls="hasSavedNote"
      :loading="hasSavedNote && !noteInfoLoaded"
      :disabled="assimilatingPropertyKey === propertyKey"
      :skipped-from-assimilation-sequence="
        isSkippedFromAssimilationSequence(noteRecallInfo, propertyKey)
      "
      :wiki-links="wikiLinks"
      :last-saved-markdown="lastSavedMarkdown"
      @assimilate="assimilate"
      @skip="skip({ propertyKey })"
      @return-to-sequence="returnToSequence({ propertyKey })"
      @dead-wiki-link-click="emit('deadWikiLinkClick', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { Minus } from "@lucide/vue"
import { computed, toRef, useId } from "vue"
import { useAssimilationView } from "@/composables/useAssimilationView"
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
import type { WikiLink } from "@generated/donut-backend-api"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"
import {
  scalarStringFromPropertyValue,
  type PropertyValue,
} from "@/utils/noteProperties"

const props = defineProps<{
  propertyKey: string
  propertyValue: PropertyValue
  noteId?: number
  wikiLinks?: WikiLink[]
  lastSavedMarkdown?: string
}>()

const emit = defineEmits<{
  remove: []
  deadWikiLinkClick: [payload: DeadWikiLinkPayload]
}>()

const {
  noteInfoLoaded,
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

const hasSavedNote = computed(() => !!props.noteId)

const authoredListValues = computed(() =>
  props.propertyValue.kind === "list" ? props.propertyValue.items : undefined
)

const showAssimilationModes = computed(
  () =>
    !isNoteLevelPropertyKey(props.propertyKey) &&
    (hasSavedNote.value || authoredListValues.value !== undefined)
)
useAssimilationView().trackPresentation(
  () => props.noteId,
  () => showAssimilationModes.value && hasSavedNote.value
)

const allowedModes: MemoryTrackerType[] = ["UNDERSTANDING"]
</script>
