<template>
  <div class="relative" @focusout="onFocusOut">
    <input
      :id="inputId"
      :value="modelValue"
      type="text"
      autocapitalize="off"
      class="daisy-input daisy-input-sm w-full min-w-[8rem] text-ellipsis"
      :aria-label="label"
      :aria-expanded="presetPanelOpen"
      :aria-controls="presetPanelOpen ? listId : undefined"
      :data-testid="testId"
      @input="onInput"
      @focus="onFocus"
      @blur="emit('blur')"
      @keydown.enter.prevent="emit('enter')"
    />
    <RichFrontmatterPropertyKeyPresets
      v-if="presetPanelOpen"
      :list-id="listId"
      :property-rows="propertyRows"
      :exclude-row-index="excludeRowIndex"
      :name-filter="typedText"
      @select="onPresetSelected"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import RichFrontmatterPropertyKeyPresets from "@/components/form/RichFrontmatterPropertyKeyPresets.vue"
import type { PropertyRow } from "@/utils/noteContentFrontmatter"

defineProps<{
  modelValue: string
  inputId: string
  listId: string
  label: string
  testId: string
  propertyRows: PropertyRow[]
  excludeRowIndex?: number
}>()

const emit = defineEmits<{
  "update:modelValue": [key: string]
  focus: []
  blur: []
  enter: []
  select: []
}>()

const presetPanelOpen = ref(false)
const typedText = ref("")

function onInput(event: Event) {
  typedText.value = (event.target as HTMLInputElement).value
  emit("update:modelValue", typedText.value)
}

function onFocus() {
  typedText.value = ""
  presetPanelOpen.value = true
  emit("focus")
}

function onFocusOut(event: FocusEvent) {
  const root = event.currentTarget as HTMLElement | null
  const next = event.relatedTarget as Node | null
  if (root?.contains(next)) return
  presetPanelOpen.value = false
}

function onPresetSelected(key: string) {
  emit("update:modelValue", key)
  presetPanelOpen.value = false
  emit("select")
}
</script>
