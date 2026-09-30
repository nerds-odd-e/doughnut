<script setup lang="ts">
import { computed, inject, unref } from "vue"
import {
  richFrontmatterIsReadmeContextFallback,
  richFrontmatterIsReadmeContextKey,
} from "@/components/form/richFrontmatterProvide"
import type { PropertyRow } from "@/utils/noteContentFrontmatter"
import { richModeKeyDropdownPresetKeysForPropertyRows } from "@/utils/noteContentFrontmatter"

const props = withDefaults(
  defineProps<{
    listId: string
    propertyRows?: PropertyRow[]
    excludeRowIndex?: number
    nameFilter?: string
  }>(),
  { propertyRows: () => [], nameFilter: "" }
)

const isReadmeContextRef = inject(
  richFrontmatterIsReadmeContextKey,
  richFrontmatterIsReadmeContextFallback
)

const presetKeys = computed(() =>
  richModeKeyDropdownPresetKeysForPropertyRows(
    unref(isReadmeContextRef),
    props.propertyRows,
    { excludeRowIndex: props.excludeRowIndex }
  ).filter((presetKey) =>
    presetKey.toLowerCase().includes(props.nameFilter.toLowerCase())
  )
)

const emit = defineEmits<{
  select: [presetKey: string]
}>()
</script>

<template>
  <ul
    v-if="presetKeys.length"
    :id="listId"
    role="listbox"
    class="donut-menu-panel daisy-menu sm:absolute sm:left-0 sm:right-0 sm:top-full z-20 mt-0.5 w-full rounded-box bg-base-100 p-1 shadow"
    data-testid="rich-note-property-key-preset-list"
  >
    <li
      v-for="presetKey in presetKeys"
      :key="presetKey"
    >
      <button
        type="button"
        role="option"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm h-auto w-full justify-start whitespace-normal break-all text-left font-mono"
        data-testid="rich-note-property-key-preset-option"
        :data-preset-key="presetKey"
        @mousedown.prevent
        @click="emit('select', presetKey)"
      >
        {{ presetKey }}
      </button>
    </li>
  </ul>
</template>
