<template>
  <div class="daisy-text-sm daisy-breadcrumbs daisy-max-w-full">
    <ul class="daisy-m-0 daisy-pl-0">
      <slot name="topLink" />
      <li
        v-for="segment in folderSegments"
        :key="'folder-' + segment.id"
      >
        <span class="daisy-text-base-content">{{ segment.name }}</span>
      </li>
      <li
        v-for="noteTopology in ancestors"
        :key="noteTopology.id"
      >
        <NoteTitleWithLink v-bind="{ noteTopology: noteTopology }" />
      </li>
      <li v-if="$slots.additional">
        <slot name="additional" />
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Folder, NoteTopology } from "@generated/doughnut-backend-api"
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"

defineProps({
  ancestors: Array as PropType<NoteTopology[]>,
  folderSegments: {
    type: Array as PropType<Folder[]>,
    default: () => [],
  },
})
</script>
