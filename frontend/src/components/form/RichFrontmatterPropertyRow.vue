<template>
  <div
    :class="[
      readOnly
        ? 'grid grid-cols-[minmax(6rem,40%)_minmax(0,1fr)] gap-x-4 gap-y-1'
        : 'flex flex-col gap-1',
      { 'rounded bg-primary/10 ring-1 ring-primary/30': isFocused },
    ]"
    data-testid="rich-note-property-row"
    :data-row-index="idx"
    :data-property-key="modelValue.key"
    :data-property-focused="isFocused ? 'true' : undefined"
    :ref="setRootRef"
  >
    <template v-if="readOnly">
      <dt class="break-words font-medium text-base-content/80">
        {{ modelValue.key }}
      </dt>
      <dd class="m-0 break-words">
        <RichFrontmatterReadOnlyPropertyValue
          :row="modelValue"
          :wiki-links="wikiLinks"
          :last-saved-markdown="lastSavedMarkdown"
        />
      </dd>
    </template>
    <div
      v-else
      class="grid grid-cols-[auto_minmax(8rem,auto)_minmax(0,1fr)] gap-x-4 items-center"
    >
      <button
        type="button"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm square shrink-0"
        :aria-label="`Toggle property panel for note property ${modelValue.key}`"
        :aria-expanded="isFocused"
        data-testid="rich-note-property-panel-toggle"
        @click="togglePropertyPanel"
      >
        <ChevronRight v-if="!isFocused" class="h-4 w-4" aria-hidden="true" />
        <ChevronDown v-else class="h-4 w-4" aria-hidden="true" />
      </button>
      <RichFrontmatterPropertyKeyField
        class="min-w-[8rem]"
        :model-value="modelValue.key"
        :input-id="keyInputId"
        :list-id="presetListId"
        :label="`Existing note property key (row ${idx + 1})`"
        test-id="rich-note-property-row-key-input"
        :property-rows="propertyRows"
        :exclude-row-index="idx"
        @update:model-value="onKeyUpdate"
        @focus="emit('row-focus')"
        @blur="emit('commit')"
        @select="focusValue"
      />
      <div ref="valueAreaRef" class="min-w-0">
        <RichFrontmatterScalarPropertyValue
          v-if="isTextCapablePropertyRow(modelValue)"
          :model-value="scalarValue"
          :property-row="modelValue"
          :wiki-links="wikiLinks"
          :last-saved-markdown="lastSavedMarkdown"
          :row-index="idx"
          @update:model-value="onValueUpdate"
          @update:property-value="onPropertyValueUpdate"
          @focus="emit('row-focus')"
          @commit="emit('commit')"
          @dead-wiki-link-click="emit('dead-wiki-link-click', $event)"
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
      </div>
    </div>
    <RichFrontmatterPropertyPanel
      v-if="isFocused && !readOnly"
      :property-key="modelValue.key"
      :note-id="noteId"
      @remove="emit('remove')"
    />
  </div>
</template>

<script setup lang="ts">
import { ChevronDown, ChevronRight } from "@lucide/vue"
import { computed, ref, type ComponentPublicInstance } from "vue"
import { useNotePropertyPanelLocation } from "@/composables/useNotePropertyPanelLocation"
import RichFrontmatterPropertyPanel from "@/components/form/RichFrontmatterPropertyPanel.vue"
import RichFrontmatterImagePropertyValue from "@/components/form/RichFrontmatterImagePropertyValue.vue"
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import RichFrontmatterPropertyKeyField from "@/components/form/RichFrontmatterPropertyKeyField.vue"
import RichFrontmatterReadOnlyPropertyValue from "@/components/form/RichFrontmatterReadOnlyPropertyValue.vue"
import RichFrontmatterScalarPropertyValue from "@/components/form/RichFrontmatterScalarPropertyValue.vue"
import RelationTypeSelectCompact from "@/components/wiki-link-or-relationship/RelationTypeSelectCompact.vue"
import type { WikiLink } from "@generated/donut-backend-api"
import {
  isImagePropertyKey,
  isRelationPropertyKey,
  isTextCapablePropertyRow,
  isWikidataIdPropertyKey,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import { scheduleFocusTargetWithin } from "@/utils/focusTarget"
import {
  scalarPropertyValue,
  scalarStringFromPropertyValue,
  type PropertyValue,
} from "@/utils/noteProperties"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"
import {
  isKnownRelationKebab,
  relationTypeFromKebab,
} from "@/models/relationTypeOptions"
const props = defineProps<{
  modelValue: PropertyRow
  idx: number
  wikiLinks: WikiLink[]
  lastSavedMarkdown?: string
  keyInputId: string
  presetListId: string
  propertyRows: PropertyRow[]
  noteId?: number
  isFocused: boolean
  readOnly?: boolean
  setRootRef: (el: Element | ComponentPublicInstance | null) => void
}>()

const emit = defineEmits<{
  "update:modelValue": [row: PropertyRow]
  "row-focus": []
  commit: []
  remove: []
  "wikidata-dialog-open": []
  "dead-wiki-link-click": [payload: DeadWikiLinkPayload]
  "relation-type-selected": [type: string | undefined]
  "image-upload-state": [inProgress: boolean]
}>()

const { togglePropertyPanel } = useNotePropertyPanelLocation(
  () => props.modelValue.key
)
const valueAreaRef = ref<HTMLElement | null>(null)

const scalarValue = computed(
  () => scalarStringFromPropertyValue(props.modelValue.value) ?? ""
)

const relationModelValue = computed(() => {
  const v = scalarValue.value
  if (isKnownRelationKebab(v)) return relationTypeFromKebab(v)
  return v.trim()
})

function onKeyUpdate(key: string) {
  emit("update:modelValue", { ...props.modelValue, key })
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

function focusValue() {
  scheduleFocusTargetWithin(valueAreaRef.value)
}
</script>
