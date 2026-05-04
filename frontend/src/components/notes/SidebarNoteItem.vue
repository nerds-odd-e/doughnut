<template>
  <li
    class="sidebar-note-li"
    role="treeitem"
    :aria-selected="activeNoteTopology != null && noteTopology.id === activeNoteTopology.id"
    :aria-label="noteTopology.title ?? undefined"
    :class="{
      'active-item':
        activeNoteTopology != null &&
        noteTopology.id === activeNoteTopology.id,
    }"
    @click.capture="onNoteRowClick"
  >
    <RouterLink
      :to="noteShowLocation(noteTopology.id)"
      class="note-row daisy-text-decoration-none"
    >
      <FileText :size="13" class="daisy-shrink-0 note-icon" />
      <NoteTitleComponent
        :class="{
          'active-title':
            activeNoteTopology != null &&
            noteTopology.id === activeNoteTopology.id,
        }"
        v-bind="{ noteTopology }"
      />
      <ScrollTo
        v-if="
          activeNoteTopology != null &&
          noteTopology.id === activeNoteTopology.id
        "
      />
    </RouterLink>
  </li>
</template>

<script setup lang="ts">
import type { NoteTopology } from "@generated/doughnut-backend-api"
import { FileText } from "lucide-vue-next"
import { RouterLink } from "vue-router"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import NoteTitleComponent from "./core/NoteTitleComponent.vue"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { sidebarTreeKey } from "./useNoteSidebarTree"
import { inject } from "vue"

const tree = inject(sidebarTreeKey)
const userActiveFolderId = tree?.userActiveFolderId

interface Props {
  noteTopology: NoteTopology
  activeNoteTopology?: NoteTopology
}

const props = defineProps<Props>()

function onNoteRowClick() {
  if (userActiveFolderId != null) {
    userActiveFolderId.value = null
  }
}
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
  padding: 0.125rem 0.25rem 0.125rem 1.5rem;
  border-radius: 0.25rem;
  box-sizing: border-box;
  color: inherit;

  &:hover {
    background-color: var(--fallback-b3, oklch(var(--b3) / 1));
  }
}

.note-icon {
  opacity: 0.45;
  flex-shrink: 0;
}

.active-item > .note-row {
  background-color: var(--fallback-b3, oklch(var(--b3) / 1));
}

.active-title {
  font-weight: 600;
}
</style>
