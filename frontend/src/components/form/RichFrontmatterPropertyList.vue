<template>
  <component :is="readOnly ? 'dl' : 'div'" class="flex flex-col gap-2 text-sm" data-testid="rich-note-property-list">
    <template v-for="(row, idx) in propertyRows" :key="rowClientIds[idx]">
    <RichFrontmatterPropertyRow
      v-model="propertyRows[idx]!"
      :idx="idx"
      :wiki-links="wikiLinks"
      :last-saved-markdown="lastSavedMarkdown"
      :note-id="noteId"
      :property-rows="propertyRows"
      :key-input-id="rowKeyInputId(idx)"
      :preset-list-id="rowKeyPresetListId(idx)"
      :is-focused="isFocusedProperty(row!.key)"
      :read-only="readOnly"
      :set-root-ref="(el) => setPropertyRowRef(row!.key, el)"
      @row-focus="emit('row-focus', idx)"
      @toggle-panel="togglePropertyPanel(row!.key)"
      @commit="emit('commit', idx)"
      @remove="emit('remove', idx)"
      @wikidata-dialog-open="emit('wikidata-dialog-open', idx)"
      @dead-wiki-link-click="emit('dead-wiki-link-click', $event)"
      @relation-type-selected="emit('relation-type-selected', idx, $event)"
      @image-upload-state="emit('image-upload-state', $event)"
    />
    <RichFrontmatterPropertyValidationMessage
      v-if="validationMessage && validationRowIndex === idx"
      :message="validationMessage"
    />
    </template>
    <RichFrontmatterPropertyRow
      v-if="draftRow"
      :model-value="draftRow"
      :idx="propertyRows.length"
      draft
      :wiki-links="wikiLinks"
      :last-saved-markdown="lastSavedMarkdown"
      :note-id="noteId"
      :property-rows="propertyRows"
      :key-input-id="insertKeyInputId!"
      :preset-list-id="insertKeyPresetListId!"
      :is-focused="false"
      :set-root-ref="() => {}"
      @update:model-value="emit('update:draftRow', $event)"
      @add="emit('add')"
      @cancel="emit('cancel')"
      @wikidata-dialog-open="emit('draft-wikidata-dialog-open')"
      @dead-wiki-link-click="emit('dead-wiki-link-click', $event)"
      @image-upload-state="emit('image-upload-state', $event)"
    />
    <RichFrontmatterPropertyValidationMessage
      v-if="validationMessage && validationRowIndex === undefined"
      :message="validationMessage"
    />
  </component>
</template>

<script setup lang="ts">
import RichFrontmatterPropertyRow from "@/components/form/RichFrontmatterPropertyRow.vue"
import RichFrontmatterPropertyValidationMessage from "@/components/form/RichFrontmatterPropertyValidationMessage.vue"
import { useFocusedNoteProperty } from "@/composables/useFocusedNoteProperty"
import { usePropertyRowClientIds } from "@/composables/usePropertyRowClientIds"
import type { WikiLink } from "@generated/donut-backend-api"
import type { PropertyRow } from "@/utils/noteContentFrontmatter"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"

const propertyRows = defineModel<PropertyRow[]>({ required: true })

const props = defineProps<{
  draftRow?: PropertyRow
  insertKeyInputId?: string
  insertKeyPresetListId?: string
  wikiLinks: WikiLink[]
  lastSavedMarkdown?: string
  noteId?: number
  readOnly?: boolean
  headingId: string
  validationMessage: string
  validationRowIndex?: number
}>()

const emit = defineEmits<{
  "row-focus": [idx: number]
  commit: [idx: number]
  "update:draftRow": [row: PropertyRow]
  add: []
  cancel: []
  "draft-wikidata-dialog-open": []
  remove: [idx: number]
  "wikidata-dialog-open": [idx: number]
  "dead-wiki-link-click": [payload: DeadWikiLinkPayload]
  "relation-type-selected": [idx: number, type: string | undefined]
  "image-upload-state": [inProgress: boolean]
}>()

const rowClientIds = usePropertyRowClientIds(propertyRows)
const { isFocusedProperty, setPropertyRowRef, togglePropertyPanel } =
  useFocusedNoteProperty()

const rowKeyInputId = (idx: number) => `${props.headingId}-row-${idx}-key`
const rowKeyPresetListId = (idx: number) =>
  `${props.headingId}-row-${idx}-key-presets`
</script>
