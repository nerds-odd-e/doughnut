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
    props.propertyRows
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
    class="daisy-menu daisy-absolute daisy-left-0 daisy-right-0 daisy-top-full daisy-z-20 daisy-mt-0.5 daisy-w-full daisy-rounded-box daisy-bg-base-100 daisy-p-1 daisy-shadow"
    data-testid="rich-note-property-key-preset-list"
  >
    <li
      v-for="presetKey in presetKeys"
      :key="presetKey"
    >
      <button
        type="button"
        role="option"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-w-full daisy-justify-start daisy-font-mono"
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
