<template>
  <li
    ref="row"
    class="sidebar-note-li"
    role="treeitem"
    :aria-selected="isActive"
    :aria-label="noteTopology.title"
    :class="{ 'active-item': isActive, 'sidebar-note-active': isActive }"
  >
    <RouterLink
      :to="noteShowLocation(noteTopology.id)"
      class="note-row no-underline"
    >
      <NoteTitleComponent v-bind="{ noteTopology }" />
    </RouterLink>
  </li>
</template>

<script setup lang="ts">
import type { NoteTopology } from "@generated/donut-backend-api"
import { RouterLink } from "vue-router"
import NoteTitleComponent from "./core/NoteTitleComponent.vue"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { computed, ref } from "vue"
import { useSidebarRowReveal } from "./useSidebarRowReveal"

interface Props {
  noteTopology: NoteTopology
  activeNoteTopology?: NoteTopology
}

const props = defineProps<Props>()

const isActive = computed(
  () => props.noteTopology.id === props.activeNoteTopology?.id
)

const row = ref<HTMLElement | null>(null)
useSidebarRowReveal(row, () => isActive.value)
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
    background-color: var(--color-base-300);
  }
}

.active-item > .note-row {
  background-color: var(--color-base-300);
}

.sidebar-note-active > .note-row {
  color: var(--color-base-content);
  background-color: color-mix(
    in oklch,
    var(--color-base-200) 78%,
    var(--color-primary) 22%
  ) !important;
  box-shadow: inset 2px 0 0 var(--color-primary);
}
</style>
