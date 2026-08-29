<template>
  <div
    v-if="unresolvedPropertyKey"
    role="alert"
    aria-live="polite"
    data-testid="rich-note-property-not-found"
    class="daisy-alert daisy-alert-warning mb-3 text-sm"
  >
    Property "{{ unresolvedPropertyKey }}" not found
  </div>
</template>

<script setup lang="ts">
import { useFocusedNoteProperty } from "@/composables/useFocusedNoteProperty"

const props = defineProps<{
  propertyKeys: string[]
}>()

const { unresolvedPropertyKey } = useFocusedNoteProperty(
  () => props.propertyKeys
)
</script>
