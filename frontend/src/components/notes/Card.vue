<template>
  <div role="card" class="card">
    <slot name="cardHeader" />
    <router-link
      :to="{ name: 'noteShow', params: { noteId: noteTopic.id } }"
      class="text-decoration-none"
    >
      <div class="card-body">
        <h5>
          <NoteTopicWithLink v-bind="{ noteTopic }" class="card-title" />
        </h5>
        <p v-if="noteTopic.shortDetails" class="note-short-details">
          {{ noteTopic.shortDetails }}
        </p>
      </div>
    </router-link>
    <div class="card-footer" v-if="$slots.button">
      <slot name="button" :note-topic="noteTopic" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { NoteTopic } from "@/generated/backend"
import NoteTopicWithLink from "./NoteTopicWithLink.vue"

defineProps({
  noteTopic: { type: Object as PropType<NoteTopic>, required: true },
})
</script>

<style scoped>
.card:hover {
  background-color: #f8f9fa !important;
}
</style>
