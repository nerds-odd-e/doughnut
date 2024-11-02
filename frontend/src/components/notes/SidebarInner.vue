<template>
  <ul v-if="(noteRealm?.children?.length ?? 0) > 0" class="list-group">
    <li
      v-for="note in noteRealm?.children"
      :key="note.id"
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
        class="d-flex w-100 justify-content-between align-items-start"
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
      </div>
      <div
        v-if="isDraggedOver === note.id && draggedNote"
        class="drop-indicator"
        role="presentation"
        aria-label="Drop position indicator"
        :style="dropIndicatorStyle"
      ></div>
      <SidebarInner
        v-if="expandedIds.some((id) => id === note.id)"
        v-bind="{
          noteId: note.id,
          activeNoteRealm,
          storageAccessor,
        }"
        :key="note.id"
      />
    </li>
  </ul>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref, watch } from "vue"
import type { Note, NoteRealm } from "@/generated/backend"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import NoteTopicWithLink from "./NoteTopicWithLink.vue"

const props = defineProps({
  noteId: { type: Number, required: true },
  activeNoteRealm: { type: Object as PropType<NoteRealm>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const noteRealm = props.storageAccessor
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(props.noteId)

const expandedIds = ref([props.activeNoteRealm.note.id])

const toggleChildren = (noteId: number) => {
  const index = expandedIds.value.indexOf(noteId)
  if (index === -1) {
    expandedIds.value.push(noteId)
  } else {
    expandedIds.value.splice(index, 1)
  }
}

const childrenCount = (noteId: number) => {
  const noteRef = props.storageAccessor.refOfNoteRealm(noteId)
  if (!noteRef.value) return undefined
  return noteRef.value.children?.length ?? 0
}

watch(
  () => props.activeNoteRealm.note.noteTopic.parentNoteTopic,
  (parentNoteTopic) => {
    const uniqueIds = new Set([
      ...expandedIds.value,
      props.activeNoteRealm.note.id,
    ])
    let cursor = parentNoteTopic
    while (cursor) {
      uniqueIds.add(cursor.id)
      cursor = cursor.parentNoteTopic
    }
    expandedIds.value = Array.from(uniqueIds)
  },
  { immediate: true }
)

const draggedNote = ref<Note | null>(null)

const dropIndicatorStyle = ref({})

const handleDragStart = (event: DragEvent, note: Note) => {
  draggedNote.value = note
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = "move"
  }
}

const handleDragOver = (event: DragEvent, targetNote: Note) => {
  event.preventDefault()
  if (
    !draggedNote.value ||
    draggedNote.value.id === targetNote.id ||
    draggedNote.value.parentId !== targetNote.parentId
  ) {
    return
  }

  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = "move"
  }

  // Calculate if we're in the upper or lower half of the target
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const mouseY = event.clientY
  const threshold = rect.top + rect.height / 2

  // Update drop indicator position
  dropIndicatorStyle.value = {
    top: mouseY >= threshold ? "100%" : "0",
    transform: mouseY >= threshold ? "translateY(-2px)" : "translateY(-1px)",
  }
}

const handleDragEnter = (_event: DragEvent, targetNote: Note) => {
  if (
    !draggedNote.value ||
    draggedNote.value.id === targetNote.id ||
    draggedNote.value.parentId !== targetNote.parentId
  ) {
    return
  }
  isDraggedOver.value = targetNote.id
}

const handleDragLeave = (event: DragEvent) => {
  // Only clear if we're actually leaving the element (not entering a child)
  const relatedTarget = event.relatedTarget as HTMLElement
  const currentTarget = event.currentTarget as HTMLElement
  if (!currentTarget.contains(relatedTarget)) {
    isDraggedOver.value = null
  }
}

const handleDrop = async (event: DragEvent, targetNote: Note) => {
  event.preventDefault()

  if (!draggedNote.value || draggedNote.value.id === targetNote.id) return

  if (draggedNote.value.parentId !== targetNote.parentId) return

  try {
    await props.storageAccessor
      .storedApi()
      .moveAfter(draggedNote.value.id, targetNote.id)
  } catch (error) {
    console.error("Failed to move note:", error)
  }

  draggedNote.value = null
}

const isDraggedOver = ref<number | null>(null)

const handleDragEnd = () => {
  draggedNote.value = null
  isDraggedOver.value = null
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

.drop-indicator {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background-color: #0d6efd;
  z-index: 1;
  pointer-events: none; // Prevent the indicator from interfering with drag events
}
</style>
