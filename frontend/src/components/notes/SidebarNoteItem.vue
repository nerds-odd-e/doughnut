<template>
  <li
    class="daisy-list-group-item daisy-list-group-item-action daisy-py-2 daisy-pb-0 daisy-pe-0 daisy-border-0"
    :class="{
      'active-item':
        activeNoteRealm != null &&
        noteRealm.id === activeNoteRealm.note.id,
      'dragging': draggedNote?.id === noteRealm.id,
    }"
    draggable="true"
    @click="onNoteRowClick"
    @dragstart="(e) => onDragStart(e, noteRealm.note)"
    @dragover.prevent="(e) => onDragOver(e, noteRealm.note)"
    @dragleave="onDragLeave"
    @drop="(e) => onDrop(e, noteRealm.note)"
    @dragend="onDragEnd"
  >
    <div
      class="daisy-flex daisy-w-full daisy-justify-between daisy-items-start note-content"
    >
      <NoteTitleWithLink
        :class="{
          'active-title':
            activeNoteRealm != null &&
            noteRealm.id === activeNoteRealm.note.id,
        }"
        v-bind="{ noteTopology: noteRealm.note.noteTopology }"
      />
      <ScrollTo v-if="activeNoteRealm != null && noteRealm.id === activeNoteRealm.note.id" />
      <div
        v-if="isDraggedOver === noteRealm.id && draggedNote"
        class="drop-indicator"
        role="presentation"
        :aria-label="dropMode === 'after' ? 'Drop position indicator' : 'Drop as child indicator'"
        :class="{ 'drop-as-child': dropMode === 'asFirstChild' }"
        :style="dropIndicatorStyle"
      ></div>
    </div>
  </li>
</template>

<script setup lang="ts">
import type { Note, NoteRealm } from "@generated/doughnut-backend-api"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import NoteTitleWithLink from "./NoteTitleWithLink.vue"
import {
  sidebarUserActiveFolderIdKey,
  sidebarUserActiveFolderSlugKey,
} from "./sidebarFolderExpansion"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { inject } from "vue"

const storageAccessor = useStorageAccessor()
const userActiveFolderSlug = inject(sidebarUserActiveFolderSlugKey, undefined)
const userActiveFolderId = inject(sidebarUserActiveFolderIdKey, undefined)

interface Props {
  note: Note
  activeNoteRealm?: NoteRealm
  draggedNote: Note | null
  isDraggedOver: number | null
  dropMode: "after" | "asFirstChild"
  dropIndicatorStyle: Record<string, string>
  onDragStart: (event: DragEvent, note: Note) => void
  onDragOver: (event: DragEvent, note: Note) => void
  onDragLeave: (event: DragEvent) => void
  onDrop: (event: DragEvent, note: Note) => void
  onDragEnd: () => void
}

const props = defineProps<Props>()

const noteRealm = storageAccessor.value.refOfNoteRealmWithFallback(props.note)

function onNoteRowClick() {
  if (userActiveFolderSlug != null) {
    userActiveFolderSlug.value = null
  }
  if (userActiveFolderId != null) {
    userActiveFolderId.value = null
  }
}
</script>

<style lang="scss" scoped>
.active-item {
  border-left: 1px solid gray !important;
}

.active-title {
  font-weight: bold;
}

.list-group-item {
  position: relative;
  border-radius: 0 !important;
  min-height: 24px; // Ensure minimum height for drag target
}

.note-content {
  position: relative;
  padding-bottom: 4px;
}

.drop-indicator {
  position: absolute;
  height: 2px;
  background-color: #0d6efd;
  z-index: 1;
  pointer-events: none;
  transition: all 0.2s ease;
  bottom: 0;

  &.drop-as-child {
    background-color: #198754;
  }
}
</style>
