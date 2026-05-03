<template>
  <ul
    v-if="displayRows.length > 0"
    class="daisy-list-group daisy-text-sm daisy-pl-[1rem]"
  >
    <template v-for="row in displayRows" :key="rowKey(row)">
      <SidebarNoteItem
        v-if="row.kind === 'note'"
        :note="row.note"
        :active-note-realm="activeNoteRealm"
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
import { sidebarStructuralRefreshKey } from "./sidebarStructuralRefresh"
import { ref, watch } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const storageAccessor = useStorageAccessor()

type SidebarStructuralRow =
  | { kind: "note"; note: Note }
  | { kind: "folder"; folder: NotebookRootFolder }

function folderNumericId(folder: NotebookRootFolder): number | undefined {
  if (folder.id == null || folder.id === "") return undefined
  return Number(folder.id)
}

function folderSortKey(folder: NotebookRootFolder): string {
  return (folder.name ?? "").toLocaleLowerCase()
}

function noteSortKey(note: Note): string {
  return (note.noteTopology?.title ?? "").toLocaleLowerCase()
}

function buildStructuralRows(
  realms: NoteRealm[],
  folders: NotebookRootFolder[] | undefined
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

  const noteRows: NoteRow[] = realms.map((realm) => ({
    kind: "note" as const,
    note: realm.note,
  }))
  noteRows.sort((a, b) =>
    noteSortKey(a.note).localeCompare(noteSortKey(b.note))
  )

  return [...folderRows, ...noteRows]
}

function rowKey(row: SidebarStructuralRow): string {
  if (row.kind === "note") {
    return `n-${row.note.id}`
  }
  const fid = folderNumericId(row.folder)
  return `f-${fid ?? "unknown"}`
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
</script>
