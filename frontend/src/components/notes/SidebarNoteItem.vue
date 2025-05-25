<template>
  <li
    class="daisy-list-group-item daisy-list-group-item-action daisy-py-2 daisy-pb-0 daisy-pe-0 daisy-border-0"
    :class="{
      'active-item': noteRealm.id === activeNoteRealm.note.id,
      'dragging': draggedNote?.id === noteRealm.id,
    }"
    draggable="true"
    @dragstart="(e) => onDragStart(e, noteRealm.note)"
    @dragover.prevent="(e) => onDragOver(e, noteRealm.note)"
    @dragenter="(e) => onDragEnter(e, noteRealm.note)"
    @dragleave="onDragLeave"
    @drop="(e) => onDrop(e, noteRealm.note)"
    @dragend="onDragEnd"
  >
    <div
      class="daisy-flex daisy-w-full daisy-justify-between daisy-items-start note-content"
      @click="toggleChildren(noteRealm.id)"
    >
      <NoteTitleWithLink
        :class="{ 'active-title': noteRealm.id === activeNoteRealm.note.id }"
        v-bind="{ noteTopology: noteRealm.note.noteTopology }"
        @click.stop
      />
      <ScrollTo v-if="noteRealm.id === activeNoteRealm.note.id" />
      <span
        role="button"
        title="expand children"
        class="daisy-badge daisy-cursor-pointer"
        >{{ childrenCount ?? "..." }}</span
      >
      <div
        v-if="isDraggedOver === noteRealm.id && draggedNote"
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
        noteId: noteRealm.id,
        activeNoteRealm,
        storageAccessor,
      }"
      :key="noteRealm.id"
    />
  </li>
</template>

<script setup lang="ts">
import type { Note, NoteRealm } from "generated/backend"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import NoteTitleWithLink from "./NoteTitleWithLink.vue"
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

const noteRealm = props.storageAccessor.refOfNoteRealmWithFallback(props.note)
const isExpanded = computed(() =>
  props.expandedIds.some((id) => id === props.note.id)
)

const childrenCount = computed(() => {
  const noteRef = props.storageAccessor.refOfNoteRealm(props.note.id)
  if (!noteRef.value) return undefined
  return noteRef.value.children?.length ?? undefined
})

const toggleChildren = (noteId: number) => {
  props.onToggleExpand(noteId)
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
