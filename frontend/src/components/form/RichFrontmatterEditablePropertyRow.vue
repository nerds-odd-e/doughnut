<template>
  <div
    class="grid grid-cols-[minmax(8rem,auto)_minmax(0,1fr)_auto] gap-x-4 gap-y-1 items-center"
    data-testid="rich-note-property-row"
    :data-row-index="idx"
    :data-property-key="modelValue.key"
  >
    <div
      class="relative min-w-[8rem]"
      @focusout="onKeyPresetWrapperFocusOut"
    >
      <input
        :id="keyInputId"
        :value="modelValue.key"
        type="text"
        autocapitalize="off"
        class="daisy-input daisy-input-sm w-full min-w-[8rem]"
        :aria-label="`Existing note property key (row ${idx + 1})`"
        :aria-expanded="presetPanelOpen"
        :aria-controls="presetPanelOpen ? presetListId : undefined"
        data-testid="rich-note-property-row-key-input"
        @input="onKeyInput"
        @focus="onKeyFocus"
        @blur="emit('commit')"
      />
      <RichFrontmatterPropertyKeyPresets
        v-if="presetPanelOpen"
        :list-id="presetListId"
        :property-rows="propertyRows"
        :exclude-row-index="idx"
        @select="onPresetSelected"
      />
    </div>
    <RichFrontmatterScalarPropertyValue
      v-if="isTextCapablePropertyRow(modelValue)"
      :model-value="scalarValue"
      :property-row="modelValue"
      :wiki-titles="wikiTitles"
      :row-index="idx"
      @update:model-value="onValueUpdate"
      @update:property-value="onPropertyValueUpdate"
      @focus="emit('row-focus')"
      @commit="emit('commit')"
      @dead-link-click="emit('dead-link-click', $event)"
    />
    <RelationTypeSelectCompact
      v-else-if="isRelationPropertyKey(modelValue.key)"
      field="relationType"
      scope-name="rich-note-relation-property"
      hide-label
      :model-value="relationModelValue"
      :inverse-icon="true"
      @update:model-value="emit('relation-type-selected', $event)"
    />
    <RichFrontmatterImagePropertyValue
      v-else-if="isImagePropertyKey(modelValue.key)"
      :model-value="scalarValue"
      :note-id="noteId"
      :ariaLabel="`Existing note image property value (row ${idx + 1})`"
      value-test-id="rich-note-property-row-value-input"
      file-input-test-id="rich-note-image-property-file-input"
      choose-button-test-id="rich-note-image-property-choose"
      requires-note-test-id="rich-note-image-upload-requires-note"
      @update:model-value="onValueUpdate"
      @focus="emit('row-focus')"
      @commit="emit('commit')"
      @image-upload-state="emit('image-upload-state', $event)"
    />
    <div
      v-else-if="isWikidataIdPropertyKey(modelValue.key)"
      class="flex min-w-0 items-center gap-2"
      :class="scalarValue.trim() ? '' : 'justify-between'"
    >
      <template v-if="scalarValue.trim()">
        <button
          type="button"
          class="daisy-btn daisy-btn-ghost daisy-btn-sm h-auto min-h-0 min-w-0 max-w-full shrink truncate justify-start py-0.5 px-1 font-mono text-sm font-normal text-base-content/90 normal-case"
          :title="scalarValue.trim()"
          data-testid="rich-note-wikidata-property-edit"
          :aria-label="`Edit Wikidata ID ${scalarValue.trim()}`"
          @click="emit('wikidata-dialog-open')"
        >
          {{ scalarValue.trim() }}
        </button>
        <RichFrontmatterPropertyExternalLink
          kind="wikidata"
          :value="scalarValue"
        />
      </template>
      <template v-else>
        <span
          class="truncate font-mono text-sm text-base-content/90"
          aria-hidden="true"
          >—</span
        >
        <button
          type="button"
          class="daisy-btn daisy-btn-sm daisy-btn-outline shrink-0"
          data-testid="rich-note-wikidata-property-edit"
          aria-label="Set Wikidata ID"
          @click="emit('wikidata-dialog-open')"
        >
          Set…
        </button>
      </template>
    </div>
    <button
      type="button"
      class="daisy-btn daisy-btn-ghost daisy-btn-sm square shrink-0"
      :aria-label="`Remove note property ${modelValue.key}`"
      data-testid="rich-note-property-row-remove"
      @click="emit('remove')"
    >
      <Minus class="h-4 w-4" aria-hidden="true" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { Minus } from "@lucide/vue"
import { computed, ref } from "vue"
import RichFrontmatterImagePropertyValue from "@/components/form/RichFrontmatterImagePropertyValue.vue"
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import RichFrontmatterPropertyKeyPresets from "@/components/form/RichFrontmatterPropertyKeyPresets.vue"
import RichFrontmatterScalarPropertyValue from "@/components/form/RichFrontmatterScalarPropertyValue.vue"
import RelationTypeSelectCompact from "@/components/links/RelationTypeSelectCompact.vue"
import type { WikiTitle } from "@generated/doughnut-backend-api"
import {
  isImagePropertyKey,
  isRelationPropertyKey,
  isTextCapablePropertyRow,
  isWikidataIdPropertyKey,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import {
  scalarPropertyValue,
  scalarStringFromPropertyValue,
  type PropertyValue,
} from "@/utils/noteProperties"
import type { DeadLinkPayload } from "@/utils/wikiPropertyValueField"
import {
  isKnownRelationKebab,
  relationTypeFromKebab,
} from "@/models/relationTypeOptions"
const props = defineProps<{
  modelValue: PropertyRow
  idx: number
  wikiTitles: WikiTitle[]
  keyInputId: string
  presetListId: string
  propertyRows: PropertyRow[]
  noteId?: number
}>()

const emit = defineEmits<{
  "update:modelValue": [row: PropertyRow]
  "row-focus": []
  commit: []
  remove: []
  "wikidata-dialog-open": []
  "dead-link-click": [payload: DeadLinkPayload]
  "relation-type-selected": [type: string | undefined]
  "image-upload-state": [inProgress: boolean]
}>()

const presetPanelOpen = ref(false)

const scalarValue = computed(
  () => scalarStringFromPropertyValue(props.modelValue.value) ?? ""
)

const relationModelValue = computed(() => {
  const v = scalarValue.value
  if (isKnownRelationKebab(v)) return relationTypeFromKebab(v)
  return v.trim()
})

function onKeyInput(event: Event) {
  emit("update:modelValue", {
    ...props.modelValue,
    key: (event.target as HTMLInputElement).value,
  })
}

function onValueUpdate(value: string) {
  emit("update:modelValue", {
    ...props.modelValue,
    value: scalarPropertyValue(value),
  })
}

function onPropertyValueUpdate(value: PropertyValue) {
  emit("update:modelValue", {
    ...props.modelValue,
    value,
  })
}

function onKeyFocus() {
  presetPanelOpen.value = true
  emit("row-focus")
}

function onKeyPresetWrapperFocusOut(event: FocusEvent) {
  const root = event.currentTarget as HTMLElement | null
  const next = event.relatedTarget as Node | null
  if (root?.contains(next)) return
  presetPanelOpen.value = false
}

function onPresetSelected(key: string) {
  emit("update:modelValue", { ...props.modelValue, key })
  presetPanelOpen.value = false
  requestAnimationFrame(() => {
    document.getElementById(props.keyInputId)?.focus()
  })
}
</script>
