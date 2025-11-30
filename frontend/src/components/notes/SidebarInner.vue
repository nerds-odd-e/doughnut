<template>
  <ul
    v-if="(noteRealm?.children?.length ?? 0) > 0"
    class="daisy-list-group daisy-text-sm daisy-pl-[1rem]"
  >
    <SidebarNoteItem
      v-for="note in noteRealm?.children"
      :key="note.id"
      v-bind="{
        note,
        activeNoteRealm,
        expandedIds,
        onToggleExpand: toggleChildren,
        draggedNote,
        isDraggedOver,
        dropMode,
        dropIndicatorStyle,
        onDragStart: handleDragStart,
        onDragOver: handleDragOver,
        onDragEnter: handleDragEnter,
        onDragLeave: handleDragLeave,
        onDrop: handleDrop,
        onDragEnd: handleDragEnd,
      }"
    />
  </ul>
</template>

<script setup lang="ts">
import type { Note, NoteRealm } from "@generated/backend"
import SidebarNoteItem from "./SidebarNoteItem.vue"
import { ref, watch } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const storageAccessor = useStorageAccessor()

interface Props {
  noteId: number
  activeNoteRealm: NoteRealm
}

const props = defineProps<Props>()

const noteRealm = storageAccessor.value
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

watch(
  () => props.activeNoteRealm.note.noteTopology.parentOrSubjectNoteTopology,
  (parentNoteTopic) => {
    const uniqueIds = new Set([
      ...expandedIds.value,
      props.activeNoteRealm.note.id,
    ])
    let cursor = parentNoteTopic
    while (cursor) {
      uniqueIds.add(cursor.id)
      cursor = cursor.parentOrSubjectNoteTopology
    }
    expandedIds.value = Array.from(uniqueIds)
  },
  { immediate: true }
)

// Drag and drop state
const draggedNote = ref<Note | null>(null)
const dropIndicatorStyle = ref({})
const dropMode = ref<"after" | "asFirstChild">("after")
const isDraggedOver = ref<number | null>(null)

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
    await storageAccessor.value
      .storedApi()
      .moveAfter(draggedNote.value.id, targetNote.id, dropMode.value)

    if (dropMode.value === "asFirstChild") {
      toggleChildren(targetNote.id)
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
