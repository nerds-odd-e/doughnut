<template>
  <div class="daisy-grid daisy-gap-3" :class="{
    'daisy-grid-cols-1': true,
    'md:daisy-grid-cols-3': columns === 4,
    'lg:daisy-grid-cols-4': columns === 4,
    'md:daisy-grid-cols-2': columns === 3,
    'lg:daisy-grid-cols-3': columns === 3,
    'md:daisy-grid-cols-1': columns === 2,
    'lg:daisy-grid-cols-2': columns === 2,
  }">
    <div v-for="noteTopology in noteTopologies" :key="noteTopology.id">
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
