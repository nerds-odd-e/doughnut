<template>
  <ul
    v-if="displayRows.length > 0"
    class="daisy-list-group daisy-text-sm daisy-pl-[1rem]"
  >
    <SidebarNoteItem
      v-for="row in displayRows"
      :key="row.note.id"
      v-bind="{
        notebookId,
        note: row.note,
        mergedFolderId: row.mergedFolderId,
        structuralChildCount: structuralChildCounts[row.note.id],
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
import type {
  Note,
  NoteRealm,
  NotebookRootFolder,
} from "@generated/doughnut-backend-api"
import SidebarNoteItem from "./SidebarNoteItem.vue"
import { ancestorTopologyIds } from "./noteTopologyAncestors"
import { sidebarStructuralRefreshKey } from "./sidebarStructuralRefresh"
import { inject, provide, ref, watch } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const storageAccessor = useStorageAccessor()
const setSidebarStructuralChildCountKey = "setSidebarStructuralChildCount"

type SidebarStructuralRow = {
  note: Note
  mergedFolderId?: number
}

function basename(path: string): string {
  const i = path.lastIndexOf("/")
  return i === -1 ? path : path.slice(i + 1)
}

function folderNumericId(folder: NotebookRootFolder): number | undefined {
  if (folder.id == null || folder.id === "") return undefined
  return Number(folder.id)
}

function mergeMatchingFoldersIntoNotes(
  notes: Note[],
  folders: NotebookRootFolder[] | undefined
): SidebarStructuralRow[] {
  const unused = [...(folders ?? [])]
  return notes.map((note) => {
    const slug = note.noteTopology.slug ?? ""
    const title = note.noteTopology.title ?? ""
    const index = unused.findIndex((folder) => {
      if (folderNumericId(folder) === undefined) return false
      return (
        folder.slug === slug ||
        folder.name === title ||
        basename(folder.slug) === basename(slug)
      )
    })
    if (index === -1) return { note }
    const [folder] = unused.splice(index, 1)
    const mergedFolderId = folder ? folderNumericId(folder) : undefined
    return mergedFolderId === undefined ? { note } : { note, mergedFolderId }
  })
}

interface Props {
  notebookId: number
  /** When omitted (e.g. notebook overview with no `index` note), root notes still render without selection */
  activeNoteRealm?: NoteRealm
  /** When set, list notes inside this folder. When omitted, list notebook root notes. */
  folderId?: number
  /** Note row whose structural children this nested listing is rendering. */
  parentRowNoteId?: number
}

const props = defineProps<Props>()

const displayRows = ref<SidebarStructuralRow[]>([])
const structuralChildCounts = ref<Record<number, number>>({})
const reportStructuralChildCountToParent = inject<
  ((parentNoteId: number, count: number) => void) | undefined
>(setSidebarStructuralChildCountKey, undefined)

const setStructuralChildCount = (parentNoteId: number, count: number) => {
  if (structuralChildCounts.value[parentNoteId] === count) return
  structuralChildCounts.value = {
    ...structuralChildCounts.value,
    [parentNoteId]: count,
  }
}

provide(setSidebarStructuralChildCountKey, setStructuralChildCount)

async function refreshListing() {
  const api = storageAccessor.value.storedApi()
  try {
    const listing =
      props.folderId == null
        ? await api.loadNotebookRootNotes(props.notebookId)
        : await api.loadFolderListing(props.notebookId, props.folderId)
    const notes = (listing.notes ?? []).map((r) => r.note)
    displayRows.value = mergeMatchingFoldersIntoNotes(notes, listing.folders)
    if (props.parentRowNoteId != null) {
      reportStructuralChildCountToParent?.(
        props.parentRowNoteId,
        displayRows.value.length
      )
    }
  } catch {
    displayRows.value = []
    if (props.parentRowNoteId != null) {
      reportStructuralChildCountToParent?.(props.parentRowNoteId, 0)
    }
  }
}

watch(
  () => [props.notebookId, props.folderId] as const,
  () => {
    if (props.folderId == null && props.parentRowNoteId == null) {
      structuralChildCounts.value = {}
    }
    refreshListing()
  },
  { immediate: true }
)

watch(sidebarStructuralRefreshKey, () => {
  refreshListing()
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
    const note = props.activeNoteRealm.note
    const parentTopic = note.noteTopology.parentOrSubjectNoteTopology

    const uniqueIds = new Set([
      ...expandedIds.value,
      note.id,
      ...ancestorTopologyIds(parentTopic),
    ])
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
    await refreshListing()
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
