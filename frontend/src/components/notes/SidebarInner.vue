<template>
  <ul
    v-if="displayRows.length > 0"
    class="daisy-list-group daisy-text-sm daisy-pl-[1rem]"
  >
    <template v-for="row in displayRows" :key="rowKey(row)">
      <SidebarNoteItem
        v-if="row.kind === 'note'"
        v-bind="{
          note: row.note,
          activeNoteRealm,
          draggedNote,
          isDraggedOver,
          dropMode,
          dropIndicatorStyle,
          onDragStart: handleDragStart,
          onDragOver: handleDragOver,
          onDragLeave: handleDragLeave,
          onDrop: handleDrop,
          onDragEnd: handleDragEnd,
        }"
      />
      <SidebarFolderItem
        v-else
        v-bind="{ notebookId, folder: row.folder, activeNoteRealm }"
      />
    </template>
  </ul>
</template>

<script setup lang="ts">
import type {
  Note,
  NoteRealm,
  NotebookRootFolder,
} from "@generated/doughnut-backend-api"
import SidebarFolderItem from "./SidebarFolderItem.vue"
import SidebarNoteItem from "./SidebarNoteItem.vue"
import type { SidebarNoteDragState } from "./sidebarNoteDragContext"
import { sidebarNoteDragStateKey } from "./sidebarNoteDragContext"
import { sidebarStructuralRefreshKey } from "./sidebarStructuralRefresh"
import { inject, ref, watch } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

function createFallbackDragState(): SidebarNoteDragState {
  return {
    draggedNote: ref(null),
    isDraggedOver: ref(null),
    dropMode: ref("after"),
    dropIndicatorStyle: ref({}),
  }
}

const storageAccessor = useStorageAccessor()

type SidebarStructuralRow =
  | { kind: "note"; note: Note }
  | { kind: "folder"; folder: NotebookRootFolder }

function folderNumericId(folder: NotebookRootFolder): number | undefined {
  if (folder.id == null || folder.id === "") return undefined
  return Number(folder.id)
}

function folderMatchesNote(folder: NotebookRootFolder, note: Note): boolean {
  if (folderNumericId(folder) === undefined) return false
  const title = note.noteTopology.title ?? ""
  return folder.name === title
}

function buildStructuralRows(
  realms: NoteRealm[],
  folders: NotebookRootFolder[] | undefined
): SidebarStructuralRow[] {
  const unused = [...(folders ?? [])]
  const rows: SidebarStructuralRow[] = []

  for (const realm of realms) {
    const note = realm.note
    rows.push({ kind: "note", note })
    const index = unused.findIndex((folder) => folderMatchesNote(folder, note))
    if (index !== -1) {
      const [folder] = unused.splice(index, 1)
      if (folder && folderNumericId(folder) !== undefined) {
        rows.push({ kind: "folder", folder })
      }
    }
  }

  for (const folder of unused) {
    if (folderNumericId(folder) !== undefined) {
      rows.push({ kind: "folder", folder })
    }
  }

  return rows
}

function rowKey(row: SidebarStructuralRow): string {
  if (row.kind === "note") {
    return `n-${row.note.id}`
  }
  const fid = folderNumericId(row.folder)
  return fid != null ? `f-${fid}` : `f-slug:${row.folder.slug}`
}

interface Props {
  notebookId: number
  /** When omitted (e.g. notebook overview with no `index` note), root notes still render without selection */
  activeNoteRealm?: NoteRealm
  /** When set, list notes inside this folder. When omitted, list notebook root notes. */
  folderId?: number
  /** Notifies enclosing folder row (when nested) how many peers this listing renders. */
  onStructuralPeerCount?: (count: number) => void
}

const props = defineProps<Props>()

const displayRows = ref<SidebarStructuralRow[]>([])

async function refreshListing() {
  const api = storageAccessor.value.storedApi()
  try {
    const listing =
      props.folderId == null
        ? await api.loadNotebookRootNotes(props.notebookId)
        : await api.loadFolderListing(props.notebookId, props.folderId)
    const realms = listing.notes ?? []
    displayRows.value = buildStructuralRows(realms, listing.folders)
    props.onStructuralPeerCount?.(displayRows.value.length)
  } catch {
    displayRows.value = []
    props.onStructuralPeerCount?.(0)
  }
}

watch(
  () => [props.notebookId, props.folderId] as const,
  () => {
    refreshListing()
  },
  { immediate: true }
)

watch(sidebarStructuralRefreshKey, () => {
  refreshListing()
})

const dragState = inject(sidebarNoteDragStateKey) ?? createFallbackDragState()
const { draggedNote, isDraggedOver, dropMode, dropIndicatorStyle } = dragState

const handleDragStart = (event: DragEvent, note: Note) => {
  draggedNote.value = note
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = "move"
  }
}

const handleDragOver = (event: DragEvent, targetNote: Note) => {
  event.preventDefault()
  if (!draggedNote.value || draggedNote.value.id === targetNote.id) {
    isDraggedOver.value = null
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
  isDraggedOver.value = targetNote.id
}

const handleDragLeave = (event: DragEvent) => {
  const relatedTarget = event.relatedTarget as HTMLElement | null
  const currentTarget = event.currentTarget as HTMLElement
  const aside = currentTarget.closest("aside")
  if (relatedTarget && aside?.contains(relatedTarget)) {
    return
  }
  isDraggedOver.value = null
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
