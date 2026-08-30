<template>
  <div class="min-w-0" :class="rowLayoutClass">
    <div
      v-if="isListValue"
      class="min-w-0 flex-1"
      data-testid="rich-note-property-row-value-input"
    >
      <RichFrontmatterListPropertyValue
        v-if="listValue"
        :value="listValue"
        :property-key="propertyKey"
        :wiki-titles="wikiTitles"
        :last-saved-markdown="lastSavedMarkdown"
        @dead-wiki-link-click="emit('dead-wiki-link-click', $event)"
      />
    </div>
    <div
      v-else
      :class="valueFieldWrapperClass"
      @pointerdown="onValuePointerDown"
    >
      <PropertyValueField
        :model-value="scalarValue"
        :wiki-titles="wikiTitles"
        :last-saved-markdown="lastSavedMarkdown"
        :aria-label="valueAriaLabel"
        data-testid="rich-note-property-row-value-input"
        @update:model-value="emit('update:modelValue', $event)"
        @focus="emit('focus')"
        @blur="emit('commit')"
        @dead-wiki-link-click="emit('dead-wiki-link-click', $event)"
      />
    </div>
    <button
      v-if="textCapable"
      type="button"
      class="daisy-btn daisy-btn-ghost daisy-btn-sm square shrink-0"
      :aria-label="`Edit property value for ${propertyKey} in dialog`"
      data-testid="rich-note-property-value-popup-open"
      @click="openValuePanel"
    >
      <SquarePen class="h-4 w-4" aria-hidden="true" />
    </button>
    <RichFrontmatterPropertyExternalLink
      v-if="isUrlProperty && scalarValue.trim()"
      kind="url"
      :value="scalarValue"
    />
    <RichFrontmatterPropertyValueDialog
      v-if="valueDialogOpen"
      :property-key="propertyKey"
      :property-value="propertyRow.value"
      :list-mode-allowed="listModeAllowed"
      @save="onValueDialogSave"
      @cancel="closeValuePanel"
    />
  </div>
</template>

<script setup lang="ts">
import { SquarePen } from "@lucide/vue"
import { computed } from "vue"
import { useNotePropertyPanelLocation } from "@/composables/useNotePropertyPanelLocation"
import RichFrontmatterListPropertyValue from "@/components/form/RichFrontmatterListPropertyValue.vue"
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import RichFrontmatterPropertyValueDialog from "@/components/form/RichFrontmatterPropertyValueDialog.vue"
import PropertyValueField from "@/components/form/PropertyValueField.vue"
import type { WikiTitle } from "@generated/donut-backend-api"
import {
  isScalarOnlyStructuralPropertyKey,
  isTextCapablePropertyRow,
  isUrlPropertyKey,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import {
  isListPropertyValue,
  scalarStringFromPropertyValue,
  type PropertyValue,
} from "@/utils/noteProperties"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"
import { primeSoftKeyboard } from "@/utils/focusTarget"

const props = defineProps<{
  modelValue: string
  propertyRow: PropertyRow
  wikiTitles: WikiTitle[]
  lastSavedMarkdown?: string
  rowIndex: number
  isFocused: boolean
}>()

const emit = defineEmits<{
  "update:modelValue": [value: string]
  "update:propertyValue": [value: PropertyValue]
  focus: []
  commit: []
  "dead-wiki-link-click": [payload: DeadWikiLinkPayload]
}>()

const propertyKey = computed(() => props.propertyRow.key)
const { replaceToPropertyPanel, replaceToNoteShow } =
  useNotePropertyPanelLocation(propertyKey)
const textCapable = computed(() => isTextCapablePropertyRow(props.propertyRow))
const valueDialogOpen = computed(() => props.isFocused && textCapable.value)
const isListValue = computed(() => isListPropertyValue(props.propertyRow.value))
const listValue = computed(() =>
  isListPropertyValue(props.propertyRow.value) ? props.propertyRow.value : null
)
const scalarValue = computed(
  () => scalarStringFromPropertyValue(props.propertyRow.value) ?? ""
)
const listModeAllowed = computed(
  () => !isScalarOnlyStructuralPropertyKey(propertyKey.value)
)
const isUrlProperty = computed(() => isUrlPropertyKey(propertyKey.value))
const usesFlexRow = computed(() => textCapable.value || isUrlProperty.value)
const rowLayoutClass = computed(() =>
  usesFlexRow.value ? "flex items-center gap-2" : ""
)
const valueFieldWrapperClass = computed(() =>
  usesFlexRow.value ? "min-w-0 flex-1" : ""
)
const valueAriaLabel = computed(
  () => `Existing note property value (row ${props.rowIndex + 1})`
)

function onValuePointerDown(event: PointerEvent) {
  const target = event.target as HTMLElement
  if (target.closest("a")) return
  primeSoftKeyboard()
}

function openValuePanel() {
  return replaceToPropertyPanel()
}

function closeValuePanel() {
  return replaceToNoteShow()
}

function onValueDialogSave(value: PropertyValue) {
  emit("update:propertyValue", value)
  emit("commit")
  return closeValuePanel()
}
</script>
