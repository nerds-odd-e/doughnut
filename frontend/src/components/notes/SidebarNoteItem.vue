<template>
  <li
    class="sidebar-note-li"
    role="treeitem"
    :aria-selected="activeNoteTopology != null && noteTopology.id === activeNoteTopology.id"
    :aria-label="noteTopology.title"
    :class="{
      'active-item':
        activeNoteTopology != null &&
        noteTopology.id === activeNoteTopology.id,
      'sidebar-note-active':
        activeNoteTopology != null &&
        noteTopology.id === activeNoteTopology.id,
    }"
  >
    <ScrollTo
      v-if="
        activeNoteTopology != null &&
        noteTopology.id === activeNoteTopology.id
      "
    />
    <RouterLink
      :to="noteShowLocation(noteTopology.id)"
      class="note-row no-underline"
    >
      <NoteTitleComponent v-bind="{ noteTopology }" />
    </RouterLink>
  </li>
</template>

<script setup lang="ts">
import type { NoteTopology } from "@generated/doughnut-backend-api"
import { RouterLink } from "vue-router"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import NoteTitleComponent from "./core/NoteTitleComponent.vue"
import { noteShowLocation } from "@/routes/noteShowLocation"

interface Props {
  noteTopology: NoteTopology
  activeNoteTopology?: NoteTopology
}

const props = defineProps<Props>()
</script>

<style lang="scss" scoped>
.sidebar-note-li {
  list-style: none;
  width: 100%;
}

.note-row {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  width: 100%;
  min-height: 2rem;
  padding: 0.125rem 0.25rem 0.125rem 1.625rem;
  border-radius: 0.25rem;
  box-sizing: border-box;
  color: inherit;

  &:hover {
    background-color: var(--fallback-b3, oklch(var(--b3) / 1));
  }
}

.active-item > .note-row {
  background-color: var(--fallback-b3, oklch(var(--b3) / 1));
}

.sidebar-note-active > .note-row {
  color: var(--fallback-bc, oklch(var(--bc) / 1));
  background-color: color-mix(
    in oklch,
    var(--fallback-b2, oklch(var(--b2) / 1)) 78%,
    var(--fallback-p, oklch(var(--p) / 1)) 22%
  ) !important;
  box-shadow: inset 2px 0 0 var(--fallback-p, oklch(var(--p) / 1));
}
</style>
