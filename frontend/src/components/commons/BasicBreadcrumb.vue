<template>
  <ol
    :style="`--bs-breadcrumb-divider: url(&#34;data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='8' height='8'%3E%3Cpath d='M2.5 0L1 1.5 3.5 4 1 6.5 2.5 8l4-4-4-4z' fill='currentColor'/%3E%3C/svg%3E&#34;);`"
    class="breadcrumb flex-nowrap"
  >
    <slot name="topLink" />
    <li
      class="breadcrumb-item text-truncate"
      v-for="noteTopology in ancestors"
      :key="noteTopology.id"
    >
      <NoteTopicWithLink v-bind="{ noteTopology: noteTopology }" />
    </li>
    <li class="breadcrumb-item" v-if="$slots.additional">
      <slot name="additional" />
    </li>
  </ol>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { NoteTopology } from "@/generated/backend"
import NoteTopicWithLink from "../notes/NoteTopicWithLink.vue"

defineProps({ ancestors: Array as PropType<NoteTopology[]> })
</script>

<style lang="scss" scoped>
.breadcrumb {
  margin-bottom: 0 !important;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
