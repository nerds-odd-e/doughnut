<template>
  <div class="flex flex-col gap-2 text-sm">
    <RichFrontmatterEditablePropertyRow
      v-for="(_, idx) in propertyRows"
      :key="rowClientIds[idx]"
      v-model="propertyRows[idx]!"
      :idx="idx"
      :wiki-titles="wikiTitles"
      :note-id="noteId"
      :property-rows="propertyRows"
      :key-input-id="rowKeyInputId(idx)"
      :preset-list-id="rowKeyPresetListId(idx)"
      :is-pending="isPendingProperty(propertyRows[idx]!.key)"
      :set-root-ref="(el) => setPropertyRowRef(propertyRows[idx]!.key, el)"
      @row-focus="emit('row-focus', idx)"
      @commit="emit('commit', idx)"
      @remove="emit('remove', idx)"
      @wikidata-dialog-open="emit('wikidata-dialog-open', idx)"
      @dead-wiki-link-click="emit('dead-wiki-link-click', $event)"
      @relation-type-selected="emit('relation-type-selected', idx, $event)"
      @image-upload-state="emit('image-upload-state', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { toRef } from "vue"
import RichFrontmatterEditablePropertyRow from "@/components/form/RichFrontmatterEditablePropertyRow.vue"
import { usePendingAssimilationProperty } from "@/composables/usePendingAssimilationProperty"
import { usePropertyRowClientIds } from "@/composables/usePropertyRowClientIds"
import type { WikiTitle } from "@generated/donut-backend-api"
import type { PropertyRow } from "@/utils/noteContentFrontmatter"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"

const propertyRows = defineModel<PropertyRow[]>({ required: true })

const props = defineProps<{
  wikiTitles: WikiTitle[]
  noteId?: number
  headingId: string
}>()

const emit = defineEmits<{
  "row-focus": [idx: number]
  commit: [idx: number]
  remove: [idx: number]
  "wikidata-dialog-open": [idx: number]
  "dead-wiki-link-click": [payload: DeadWikiLinkPayload]
  "relation-type-selected": [idx: number, type: string | undefined]
  "image-upload-state": [inProgress: boolean]
}>()

const rowClientIds = usePropertyRowClientIds(propertyRows)
const { isPendingProperty, setPropertyRowRef } = usePendingAssimilationProperty(
  toRef(() => props.noteId ?? 0)
)

const rowKeyInputId = (idx: number) => `${props.headingId}-row-${idx}-key`
const rowKeyPresetListId = (idx: number) =>
  `${props.headingId}-row-${idx}-key-presets`
</script>
