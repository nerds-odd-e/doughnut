<template>
  <div role="card" class="card">
    <slot name="cardHeader" />
    <router-link
      :to="{ name: 'noteShow', params: { noteId: noteTopology.id } }"
      class="text-decoration-none"
    >
      <div class="card-body">
        <h5>
          <NoteTopicWithLink v-bind="{ noteTopology }" class="card-title" />
        </h5>
        <p v-if="noteTopology.shortDetails" class="note-short-details">
          {{ noteTopology.shortDetails }}
        </p>
      </div>
    </router-link>
    <div class="card-footer" v-if="$slots.button">
      <slot name="button" :note-title="noteTopology" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { NoteTopology } from "@/generated/backend"
import NoteTopicWithLink from "./NoteTopicWithLink.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
})
</script>

<style scoped>
.card:hover {
  background-color: #f8f9fa !important;
}
</style>
