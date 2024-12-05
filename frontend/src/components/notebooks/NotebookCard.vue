<template>
  <div role="card" class="card">
    <slot name="cardHeader" />
    <router-link
      :to="{ name: 'noteShow', params: { noteId: notebook.headNoteTopic.id } }"
      class="text-decoration-none"
    >
      <div class="card-body">
        <h5 class="card-title">
          {{ notebook.headNoteTopic.titleOrPredicate }}
        </h5>
        <p v-if="notebook.headNoteTopic.shortDetails" class="note-short-details">
          {{ notebook.headNoteTopic.shortDetails }}
        </p>
      </div>
    </router-link>
    <div class="card-footer" v-if="$slots.button">
      <slot name="button" :notebook="notebook" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Notebook } from "@/generated/backend"

defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
})
</script>

<style scoped>
.card:hover {
  background-color: #f8f9fa !important;
}
</style>
