<template>
  <ul
    class="flex flex-col gap-2"
    data-testid="accidental-match-resolve-dialog"
  >
    <AccidentalMatchResolveRow
      v-for="matched in matchedNotes"
      :key="matched.id"
      :matched="matched"
    />
  </ul>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import AccidentalMatchResolveRow from "@/components/recall/AccidentalMatchResolveRow.vue"

defineProps({
  matchedNotes: {
    type: Array as PropType<NoteTopology[]>,
    required: true,
  },
})
</script>
