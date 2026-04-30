<template>
  <ul
    v-if="(displayNotes?.length ?? 0) > 0"
    class="daisy-list-group daisy-text-sm daisy-pl-[1rem]"
  >
    <SidebarNoteItem
      v-for="note in displayNotes"
      :key="note.id"
      v-bind="{
        notebookId,
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
import type { Note, NoteRealm } from "@generated/doughnut-backend-api"
import SidebarNoteItem from "./SidebarNoteItem.vue"
import { computed, ref, watch } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const storageAccessor = useStorageAccessor()

interface Props {
  notebookId: number
  /** When omitted (e.g. notebook overview with no `index` note), root notes still render without selection */
  activeNoteRealm?: NoteRealm
  /** When set, list this note's children. When omitted, list notebook root notes. */
  noteId?: number
}

const props = defineProps<Props>()

const subtreeRealmRef =
  props.noteId !== undefined
    ? storageAccessor.value
        .storedApi()
        .getNoteRealmRefAndLoadWhenNeeded(props.noteId)
    : undefined

const rootNotesList = ref<Note[]>([])

async function loadNotebookRootNotesList() {
  if (props.noteId !== undefined) return
  try {
    const realms = await storageAccessor.value
      .storedApi()
      .loadNotebookRootNotes(props.notebookId)
    rootNotesList.value = realms.map((r) => r.note)
  } catch {
    rootNotesList.value = []
  }
}

watch(
  () => props.notebookId,
  () => {
    if (props.noteId === undefined) {
      loadNotebookRootNotesList()
    }
  },
  { immediate: true }
)

watch(
  () => ({
    activeNoteId: props.activeNoteRealm?.note?.id,
    parentTopic:
      props.activeNoteRealm?.note?.noteTopology.parentOrSubjectNoteTopology,
  }),
  () => {
    if (props.noteId !== undefined) return
    if (!props.activeNoteRealm?.note) return
    const storedApi = storageAccessor.value.storedApi()
    let cursor =
      props.activeNoteRealm.note.noteTopology.parentOrSubjectNoteTopology
    while (cursor) {
      storedApi.getNoteRealmRefAndLoadWhenNeeded(cursor.id)
      cursor = cursor.parentOrSubjectNoteTopology
    }
  },
  { immediate: true }
)

const displayNotes = computed(() => {
  if (props.noteId !== undefined && subtreeRealmRef) {
    return subtreeRealmRef.value?.children ?? []
  }
  return rootNotesList.value
})

const expandedIds = ref<number[]>([])

watch(
  () => ({
    activeNoteId: props.activeNoteRealm?.note?.id,
    parentTopic:
      props.activeNoteRealm?.note?.noteTopology.parentOrSubjectNoteTopology,
  }),
  () => {
    if (!props.activeNoteRealm?.note) {
      return
    }
    const parentNoteTopic =
      props.activeNoteRealm.note.noteTopology.parentOrSubjectNoteTopology
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

const toggleChildren = (noteId: number) => {
  const index = expandedIds.value.indexOf(noteId)
  if (index === -1) {
    expandedIds.value.push(noteId)
  } else {
    expandedIds.value.splice(index, 1)
  }
}

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
    if (props.noteId === undefined) {
      await loadNotebookRootNotesList()
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
