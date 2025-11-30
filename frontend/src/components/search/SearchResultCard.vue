<template>
  <div role="card" class="daisy-card daisy-bg-base-100 daisy-shadow-xl hover:daisy-shadow-2xl hover:daisy-bg-base-300 daisy-transition-all">
    <div class="daisy-card-body daisy-p-4">
      <router-link
        :to="{ name: 'noteShow', params: { noteId: searchResult.noteTopology.id } }"
        class="daisy-no-underline"
      >
        <h5 class="daisy-card-title">
          {{ searchResult.noteTopology.titleOrPredicate }}
        </h5>
      </router-link>
      <div class="daisy-card-actions daisy-justify-end" v-if="$slots.button">
        <slot name="button" :search-result="searchResult" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteSearchResult } from "@generated/backend"

defineProps({
  searchResult: { type: Object as PropType<NoteSearchResult>, required: true },
})
</script>

