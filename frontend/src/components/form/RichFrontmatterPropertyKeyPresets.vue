<script setup lang="ts">
import { computed, inject, unref } from "vue"
import {
  richFrontmatterIsIndexContextFallback,
  richFrontmatterIsIndexContextKey,
} from "@/components/form/richFrontmatterProvide"
import type { PropertyRow } from "@/utils/noteContentFrontmatter"
import { richModeKeyDropdownPresetKeysForPropertyRows } from "@/utils/noteContentFrontmatter"

const props = withDefaults(
  defineProps<{
    listId: string
    propertyRows?: PropertyRow[]
    excludeRowIndex?: number
  }>(),
  { propertyRows: () => [] }
)

const isIndexContextRef = inject(
  richFrontmatterIsIndexContextKey,
  richFrontmatterIsIndexContextFallback
)

const presetKeys = computed(() =>
  richModeKeyDropdownPresetKeysForPropertyRows(
    unref(isIndexContextRef),
    props.propertyRows,
    { excludeRowIndex: props.excludeRowIndex }
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
    class="doughnut-menu-panel daisy-menu absolute left-0 right-0 top-full z-20 mt-0.5 w-full rounded-box bg-base-100 p-1 shadow"
    data-testid="rich-note-property-key-preset-list"
  >
    <li
      v-for="presetKey in presetKeys"
      :key="presetKey"
    >
      <button
        type="button"
        role="option"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm w-full justify-start font-mono"
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
