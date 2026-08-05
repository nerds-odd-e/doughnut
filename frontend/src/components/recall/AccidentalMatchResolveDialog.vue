<template>
  <ul
    class="flex flex-col gap-2"
    data-testid="accidental-match-resolve-dialog"
  >
    <li
      v-for="matched in matchedNotes"
      :key="matched.id"
      :data-testid="`resolve-match-row-${matched.id}`"
    >
      {{ matched.title }}
    </li>
  </ul>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"

defineProps({
  matchedNotes: {
    type: Array as PropType<NoteTopology[]>,
    required: true,
  },
})
</script>
