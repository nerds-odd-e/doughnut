<template>
  <div class="min-w-0" :class="rowLayoutClass">
    <div :class="valueFieldWrapperClass" @pointerdown="onValuePointerDown">
      <WikiPropertyValueField
        :model-value="modelValue"
        :wiki-titles="wikiTitles"
        :aria-label="valueAriaLabel"
        data-testid="rich-note-property-row-value-input"
        @update:model-value="emit('update:modelValue', $event)"
        @focus="emit('focus')"
        @blur="emit('commit')"
        @dead-link-click="emit('dead-link-click', $event)"
      />
    </div>
    <button
      v-if="textCapable"
      type="button"
      class="daisy-btn daisy-btn-ghost daisy-btn-sm square shrink-0"
      :aria-label="`Edit property value for ${propertyKey} in dialog`"
      data-testid="rich-note-property-value-popup-open"
      @click="valuePopupOpen = true"
    >
      <SquarePen class="h-4 w-4" aria-hidden="true" />
    </button>
    <RichFrontmatterPropertyExternalLink
      v-if="isUrlProperty && modelValue.trim()"
      kind="url"
      :value="modelValue"
    />
    <RichFrontmatterScalarPropertyValueDialog
      v-if="valuePopupOpen"
      :property-key="propertyKey"
      :model-value="modelValue"
      @save="onValuePopupSave"
      @cancel="valuePopupOpen = false"
    />
  </div>
</template>

<script setup lang="ts">
import { SquarePen } from "@lucide/vue"
import { computed, ref } from "vue"
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import RichFrontmatterScalarPropertyValueDialog from "@/components/form/RichFrontmatterScalarPropertyValueDialog.vue"
import WikiPropertyValueField from "@/components/form/WikiPropertyValueField.vue"
import type { WikiTitle } from "@generated/doughnut-backend-api"
import {
  isTextCapableScalarPropertyRow,
  isUrlPropertyKey,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import type { DeadLinkPayload } from "@/utils/wikiPropertyValueField"
import { primeSoftKeyboard } from "@/utils/focusTarget"

const props = defineProps<{
  modelValue: string
  propertyRow: PropertyRow
  wikiTitles: WikiTitle[]
  rowIndex: number
}>()

const emit = defineEmits<{
  "update:modelValue": [value: string]
  focus: []
  commit: []
  "dead-link-click": [payload: DeadLinkPayload]
}>()

const valuePopupOpen = ref(false)

const propertyKey = computed(() => props.propertyRow.key)
const textCapable = computed(() =>
  isTextCapableScalarPropertyRow(props.propertyRow)
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

function onValuePopupSave(value: string) {
  emit("update:modelValue", value)
  valuePopupOpen.value = false
  emit("commit")
}
</script>
