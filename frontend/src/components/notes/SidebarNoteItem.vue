<template>
  <li
    class="list-group-item list-group-item-action pb-0 pe-0 border-0"
    :class="{
      'active-item': note.id === activeNoteRealm.note.id,
      'dragging': draggedNote?.id === note.id,
    }"
    draggable="true"
    @dragstart="(e) => onDragStart(e, note)"
    @dragover.prevent="(e) => onDragOver(e, note)"
    @dragenter="(e) => onDragEnter(e, note)"
    @dragleave="onDragLeave"
    @drop="(e) => onDrop(e, note)"
    @dragend="onDragEnd"
  >
    <div
      class="d-flex w-100 justify-content-between align-items-start note-content"
      @click="toggleChildren(note.id)"
    >
      <NoteTopicWithLink
        class="card-title"
        :class="{ 'active-topic': note.id === activeNoteRealm.note.id }"
        v-bind="{ noteTopic: note.noteTopic }"
        @click.stop
      />
      <ScrollTo v-if="note.id === activeNoteRealm.note.id" />
      <span
        role="button"
        title="expand children"
        class="badge rounded-pill"
        >{{ childrenCount ?? "..." }}</span
      >
      <div
        v-if="isDraggedOver === note.id && draggedNote"
        class="drop-indicator"
        role="presentation"
        :aria-label="dropMode === 'after' ? 'Drop position indicator' : 'Drop as child indicator'"
        :class="{ 'drop-as-child': dropMode === 'asFirstChild' }"
        :style="dropIndicatorStyle"
      ></div>
    </div>
    <SidebarInner
      v-if="isExpanded"
      v-bind="{
        noteId: note.id,
        activeNoteRealm,
        storageAccessor,
      }"
      :key="note.id"
    />
  </li>
</template>

<script setup lang="ts">
import type { Note, NoteRealm } from "@/generated/backend"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import NoteTopicWithLink from "./NoteTopicWithLink.vue"
import SidebarInner from "./SidebarInner.vue"
import { computed } from "vue"
import type { StorageAccessor } from "@/store/createNoteStorage"

interface Props {
  note: Note
  activeNoteRealm: NoteRealm
  storageAccessor: StorageAccessor
  expandedIds: number[]
  onToggleExpand: (noteId: number) => void
  draggedNote: Note | null
  isDraggedOver: number | null
  dropMode: "after" | "asFirstChild"
  dropIndicatorStyle: Record<string, string>
  onDragStart: (event: DragEvent, note: Note) => void
  onDragOver: (event: DragEvent, note: Note) => void
  onDragEnter: (event: DragEvent, note: Note) => void
  onDragLeave: (event: DragEvent) => void
  onDrop: (event: DragEvent, note: Note) => void
  onDragEnd: () => void
}

const props = defineProps<Props>()

const isExpanded = computed(() =>
  props.expandedIds.some((id) => id === props.note.id)
)

const childrenCount = computed(() => {
  const noteRef = props.storageAccessor.refOfNoteRealm(props.note.id)
  if (!noteRef.value) return undefined
  return noteRef.value.children?.length ?? 0
})

const toggleChildren = (noteId: number) => {
  props.onToggleExpand(noteId)
}
</script>

<style lang="scss" scoped>
.active-item {
  border-left: 1px solid gray !important;
}

.active-topic {
  font-weight: bold;
}

.list-group-item {
  position: relative;
  border-radius: 0 !important;
  min-height: 24px; // Ensure minimum height for drag target
}

.badge {
  cursor: pointer;
  background-color: #aaa;
  font-weight: initial;
}

.note-item {
  cursor: move;
  padding: 4px;
  transition: background-color 0.2s;
}

.note-item.dragging {
  opacity: 0.5;
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
