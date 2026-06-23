<template>
  <span
    class="block min-w-0 truncate text-sm text-base-content/90"
    :title="title"
    data-testid="rich-note-property-row-list-value"
  >{{ compactDisplayForPropertyValue(value) }}</span>
</template>

<script setup lang="ts">
import { computed } from "vue"
import {
  compactDisplayForPropertyValue,
  type PropertyValue,
} from "@/utils/noteProperties"

const props = defineProps<{
  value: Extract<PropertyValue, { kind: "list" }>
}>()

const title = computed(() =>
  props.value.items.length === 0 ? "[]" : props.value.items.join("\n")
)
</script>
