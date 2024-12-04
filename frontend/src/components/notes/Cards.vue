<template>
  <div
    :class="`row row-cols-1 row-cols-md-${columns - 1} row-cols-lg-${columns} g-3`"
  >
    <div class="col" v-for="noteTopology in noteTopologies" :key="noteTopology.id">
      <Card v-bind="{ noteTopology: noteTopology }">
        <template #button v-if="$slots.button">
          <slot name="button" :note-topology="noteTopology" />
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { NoteTopology } from "@/generated/backend"
import Card from "./Card.vue"

defineProps({
  noteTopologies: { type: Array as PropType<NoteTopology[]>, required: true },
  columns: { type: Number, default: 4 },
})
</script>
