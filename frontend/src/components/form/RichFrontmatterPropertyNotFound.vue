<template>
  <div v-if="unresolvedPropertyKey || unresolvedItem" role="alert" aria-live="polite"
    :data-testid="unresolvedPropertyKey ? 'rich-note-property-not-found' : 'rich-note-property-item-not-found'"
    class="daisy-alert daisy-alert-warning mb-3 text-sm">
    <template v-if="unresolvedPropertyKey">Property "{{ unresolvedPropertyKey }}" not found</template>
    <template v-else>Item "{{ focusedProperty?.item?.value }}" (occurrence {{ focusedProperty?.item?.occurrence }}) not found in property "{{ focusedProperty?.key }}"</template>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { useFocusedNoteProperty } from "@/composables/useFocusedNoteProperty"
import type { PropertyRow } from "@/utils/noteContentFrontmatter"
const props = defineProps<{ propertyRows: PropertyRow[] }>()
const { unresolvedPropertyKey, focusedProperty, focusedItemIndex } =
  useFocusedNoteProperty(() => props.propertyRows.map((row) => row.key))
const unresolvedItem = computed(() => {
  const focus = focusedProperty.value
  if (!focus?.item || unresolvedPropertyKey.value) return false
  const row = props.propertyRows.find((row) => row.key === focus.key)
  return (
    row?.value.kind !== "list" ||
    focusedItemIndex(focus.key, row.value.items) < 0
  )
})
</script>
