<template>
  <div
    class="list-group-item list-group-item-action pb-0 pe-0 border-0"
    :class="{
      'active-item': note.id === activeNoteRealm.note.id,
      'dragging': draggedNote?.id === note.id,
    }"
    draggable="true"
    @dragstart="handleDragStart($event, note)"
    @dragover.prevent="handleDragOver($event, note)"
    @dragenter="handleDragEnter($event, note)"
    @dragleave="handleDragLeave($event)"
    @drop="handleDrop($event, note)"
    @dragend="handleDragEnd"
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
        >{{ childrenCount(note.id) ?? "..." }}</span
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
      v-if="expandedIds.some((id) => id === note.id)"
      v-bind="{
        noteId: note.id,
        activeNoteRealm,
        storageAccessor,
      }"
      :key="note.id"
    />
  </div>
</template>

<script setup lang="ts">
import type { Note, NoteRealm } from "@/generated/backend"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import NoteTopicWithLink from "./NoteTopicWithLink.vue"
import SidebarInner from "./SidebarInner.vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import { ref } from "vue"

interface Props {
  note: Note
  activeNoteRealm: NoteRealm
  storageAccessor: StorageAccessor
  expandedIds: number[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: "toggleChildren", noteId: number): void
}>()

const draggedNote = ref<Note | null>(null)
const dropIndicatorStyle = ref({})
const dropMode = ref<"after" | "asFirstChild">("after")
const isDraggedOver = ref<number | null>(null)

const childrenCount = (noteId: number) => {
  const noteRef = props.storageAccessor.refOfNoteRealm(noteId)
  if (!noteRef.value) return undefined
  return noteRef.value.children?.length ?? 0
}

const toggleChildren = (noteId: number) => {
  emit("toggleChildren", noteId)
}

// Drag and drop handlers remain the same
const handleDragStart = (event: DragEvent, note: Note) => {
  draggedNote.value = note
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = "move"
  }
}

const handleDragOver = (event: DragEvent, targetNote: Note) => {
  event.preventDefault()
  if (!draggedNote.value || draggedNote.value.id === targetNote.id) {
    return
  }

  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = "move"
  }

  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const mouseX = event.clientX - rect.left
  const isRightHalf = mouseX > rect.width / 2

  dropMode.value = isRightHalf ? "asFirstChild" : "after"

  if (
    dropMode.value === "after" &&
    draggedNote.value.parentId !== targetNote.parentId
  ) {
    isDraggedOver.value = null
    return
  }

  dropIndicatorStyle.value = {
    top: "100%",
    transform: "translateY(-2px)",
    ...(dropMode.value === "asFirstChild"
      ? {
          left: "20px",
          right: "0",
        }
      : {
          left: "0",
          right: "0",
        }),
  }
}

const handleDragEnter = (_event: DragEvent, targetNote: Note) => {
  if (!draggedNote.value || draggedNote.value.id === targetNote.id) {
    return
  }
  isDraggedOver.value = targetNote.id
}

const handleDragLeave = (event: DragEvent) => {
  const relatedTarget = event.relatedTarget as HTMLElement
  const currentTarget = event.currentTarget as HTMLElement
  if (!currentTarget.contains(relatedTarget)) {
    isDraggedOver.value = null
  }
}

const handleDrop = async (event: DragEvent, targetNote: Note) => {
  event.preventDefault()

  if (!draggedNote.value || draggedNote.value.id === targetNote.id) return

  if (
    dropMode.value === "after" &&
    draggedNote.value.parentId !== targetNote.parentId
  )
    return

  try {
    await props.storageAccessor
      .storedApi()
      .moveAfter(draggedNote.value.id, targetNote.id, dropMode.value)

    if (dropMode.value === "asFirstChild") {
      emit("toggleChildren", targetNote.id)
    }
  } catch (error) {
    console.error("Failed to move note:", error)
  }

  draggedNote.value = null
  dropMode.value = "after"
}

const handleDragEnd = () => {
  draggedNote.value = null
  isDraggedOver.value = null
  dropMode.value = "after"
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
  min-height: 24px;
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
