<template>
  <div class="flex flex-col gap-2 text-sm">
    <RichFrontmatterEditablePropertyRow
      v-for="(row, idx) in propertyRows"
      :key="rowClientIds[idx]"
      v-model="propertyRows[idx]!"
      :idx="idx"
      :wiki-links="wikiLinks"
      :last-saved-markdown="lastSavedMarkdown"
      :note-id="noteId"
      :property-rows="propertyRows"
      :key-input-id="rowKeyInputId(idx)"
      :preset-list-id="rowKeyPresetListId(idx)"
      :is-focused="isFocusedProperty(row!.key)"
      :set-root-ref="(el) => setPropertyRowRef(row!.key, el)"
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
import RichFrontmatterEditablePropertyRow from "@/components/form/RichFrontmatterEditablePropertyRow.vue"
import { useFocusedNoteProperty } from "@/composables/useFocusedNoteProperty"
import { usePropertyRowClientIds } from "@/composables/usePropertyRowClientIds"
import type { WikiLink } from "@generated/donut-backend-api"
import type { PropertyRow } from "@/utils/noteContentFrontmatter"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"

const propertyRows = defineModel<PropertyRow[]>({ required: true })

const props = defineProps<{
  wikiLinks: WikiLink[]
  lastSavedMarkdown?: string
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
const { isFocusedProperty, setPropertyRowRef } = useFocusedNoteProperty()

const rowKeyInputId = (idx: number) => `${props.headingId}-row-${idx}-key`
const rowKeyPresetListId = (idx: number) =>
  `${props.headingId}-row-${idx}-key-presets`
</script>
