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
    @click="onNoteRowClick"
  >
    <div class="note-row">
      <FileText :size="13" class="daisy-shrink-0 note-icon" />
      <NoteTitleWithLink
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
    </div>
  </li>
</template>

<script setup lang="ts">
import type { NoteTopology } from "@generated/doughnut-backend-api"
import { FileText } from "lucide-vue-next"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import NoteTitleWithLink from "./NoteTitleWithLink.vue"
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
}

.note-row {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  min-height: 2rem;
  padding: 0.125rem 0.25rem 0.125rem 1.5rem;
  border-radius: 0.25rem;

  &:hover {
    background-color: var(--color-base-300, rgba(0, 0, 0, 0.08));
  }
}

.note-icon {
  opacity: 0.45;
  flex-shrink: 0;
}

.active-item > .note-row {
  background-color: var(--color-base-300, rgba(0, 0, 0, 0.08));
}

.active-title {
  font-weight: 600;
}
</style>
