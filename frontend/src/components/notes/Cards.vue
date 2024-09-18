<template>
  <div
    :class="`row row-cols-1 row-cols-md-${columns - 1} row-cols-lg-${columns} g-3`"
  >
    <div class="col" v-for="noteTopic in noteTopics" :key="noteTopic.id">
      <Card v-bind="{ noteTopic }">
        <template #button v-if="$slots.button">
          <slot name="button" :note-topic="noteTopic" />
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { NoteTopic } from "@/generated/backend"
import Card from "./Card.vue"

defineProps({
  noteTopics: { type: Array as PropType<NoteTopic[]>, required: true },
  columns: { type: Number, default: 4 },
})
</script>
