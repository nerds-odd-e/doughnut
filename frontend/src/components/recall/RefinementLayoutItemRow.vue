<template>
  <label class="flex items-start cursor-pointer gap-2 flex-1 min-w-0">
    <input
      type="checkbox"
      :checked="fullySelected"
      :indeterminate="partiallySelected"
      :aria-label="`Select ${item.text}`"
      :data-test-id="`refinement-layout-checkbox-${item.id}`"
      @change="
        $emit('selectionChange', ($event.target as HTMLInputElement).checked)
      "
      class="daisy-checkbox daisy-checkbox-accent daisy-checkbox-sm mt-1 border-black dark:border-white hover:border-black hover:dark:border-white checked:border-black checked:dark:border-white border-2 shrink-0"
    />
    <span class="break-words">
      {{ item.text }}
      <span
        v-if="item.alreadyExtracted"
        class="daisy-badge daisy-badge-sm daisy-badge-outline ml-2 align-middle"
      >
        Already extracted
      </span>
    </span>
  </label>
</template>

<script setup lang="ts">
import type { NoteRefinementLayoutItem } from "@generated/doughnut-backend-api"

defineProps<{
  item: NoteRefinementLayoutItem
  fullySelected: boolean
  partiallySelected: boolean
}>()

defineEmits<{
  (e: "selectionChange", selected: boolean): void
}>()
</script>
