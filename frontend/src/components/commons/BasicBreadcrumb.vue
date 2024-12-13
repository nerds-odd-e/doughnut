<template>
  <div class="text-sm daisy-breadcrumbs">
    <ul>
      <slot name="topLink" />
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
import { NoteTopology } from "@/generated/backend"
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"

defineProps({ ancestors: Array as PropType<NoteTopology[]> })
</script>

<style lang="scss" scoped>
.daisy-breadcrumbs {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
