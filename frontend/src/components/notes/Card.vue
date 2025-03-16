<template>
  <div role="card" class="daisy:card daisy:bg-base-100 daisy:shadow-xl daisy:hover:shadow-2xl daisy:hover:bg-base-300 daisy:transition-all">
    <slot name="cardHeader" />
      <div class="daisy:card-body daisy:p-4">
    <router-link
      :to="{ name: 'noteShow', params: { noteId: noteTopology.id } }"
      class="daisy:no-underline"
    >
        <h5 class="daisy:card-title">
          <NoteTitleWithLink v-bind="{ noteTopology }" />
        </h5>
        <p v-if="noteTopology.shortDetails" class="daisy:text-base">
          {{ noteTopology.shortDetails }}
        </p>
    </router-link>
    <div class="daisy:card-actions daisy:justify-end" v-if="$slots.button">
      <slot name="button" :note-title="noteTopology" />
    </div>
      </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { NoteTopology } from "@/generated/backend"
import NoteTitleWithLink from "./NoteTitleWithLink.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
})
</script>
