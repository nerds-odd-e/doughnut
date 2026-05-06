<template>
  <ul
    v-if="displayRows.length > 0"
    class="sidebar-tree-list"
    :role="currentLevel === 1 ? 'tree' : 'group'"
    :aria-label="currentLevel === 1 ? 'Note tree' : undefined"
  >
    <template v-for="row in displayRows" :key="rowKey(row)">
      <SidebarNoteItem
        v-if="row.kind === 'note'"
        :note-topology="row.noteTopology"
        :active-note-topology="activeNoteTopology"
        :aria-level="currentLevel"
      />
      <SidebarFolderItem
        v-else
        v-bind="{ notebookId, folder: row.folder, activeNoteTopology, level: currentLevel }"
      />
    </template>
  </ul>
</template>

<script setup lang="ts">
import type { NoteTopology, Folder } from "@generated/doughnut-backend-api"
import SidebarFolderItem from "./SidebarFolderItem.vue"
import SidebarNoteItem from "./SidebarNoteItem.vue"
import { sidebarStructuralRefreshKey } from "./sidebarStructuralRefresh"
import { computed, ref, watch } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const storageAccessor = useStorageAccessor()

type SidebarStructuralRow =
  | { kind: "note"; noteTopology: NoteTopology }
  | { kind: "folder"; folder: Folder }

function folderNumericId(folder: Folder): number | undefined {
  return folder.id
}

function folderSortKey(folder: Folder): string {
  return (folder.name ?? "").toLocaleLowerCase()
}

function noteSortKey(noteTopology: NoteTopology): string {
  return (noteTopology.title ?? "").toLocaleLowerCase()
}

function buildStructuralRows(
  noteTopologies: NoteTopology[],
  folders: Folder[] | undefined
): SidebarStructuralRow[] {
  type FolderRow = Extract<SidebarStructuralRow, { kind: "folder" }>
  type NoteRow = Extract<SidebarStructuralRow, { kind: "note" }>

  const folderRows: FolderRow[] = []
  for (const folder of folders ?? []) {
    if (folderNumericId(folder) !== undefined) {
      folderRows.push({ kind: "folder", folder })
    }
  }
  folderRows.sort((a, b) =>
    folderSortKey(a.folder).localeCompare(folderSortKey(b.folder))
  )

  const noteRows: NoteRow[] = noteTopologies.map((noteTopology) => ({
    kind: "note" as const,
    noteTopology,
  }))
  noteRows.sort((a, b) =>
    noteSortKey(a.noteTopology).localeCompare(noteSortKey(b.noteTopology))
  )

  return [...folderRows, ...noteRows]
}

function rowKey(row: SidebarStructuralRow): string {
  if (row.kind === "note") {
    return `n-${row.noteTopology.id}`
  }
  const fid = folderNumericId(row.folder)
  return `f-${fid ?? "unknown"}`
}

interface Props {
  notebookId: number
  /** When omitted (e.g. notebook overview with no `index` note), root notes still render without selection */
  activeNoteTopology?: NoteTopology
  /** When set, list notes inside this folder. When omitted, list notebook root notes. */
  folderId?: number
  /** Notifies enclosing folder row (when nested) how many peers this listing renders. */
  onStructuralPeerCount?: (count: number) => void
  /** ARIA level for treeitem descendants. Defaults to 1 (root tree). */
  level?: number
}

const props = defineProps<Props>()

const currentLevel = computed(() => props.level ?? 1)

const displayRows = ref<SidebarStructuralRow[]>([])

async function refreshListing() {
  const api = storageAccessor.value.storedApi()
  try {
    const listing =
      props.folderId == null
        ? await api.loadNotebookRootNotes(props.notebookId)
        : await api.loadFolderListing(props.notebookId, props.folderId)
    const noteTopologies = listing.noteTopologies ?? []
    displayRows.value = buildStructuralRows(noteTopologies, listing.folders)
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
</script>

<style lang="scss" scoped>
.sidebar-tree-list {
  list-style: none;
  padding: 0;
  margin: 0;
  font-size: 0.875rem;
}
</style>
