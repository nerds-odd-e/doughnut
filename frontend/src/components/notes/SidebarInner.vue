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
        v-bind="{
          notebookId,
          folder: row.folder,
          activeNoteTopology,
          level: currentLevel,
          expandedFolderIds,
          activePathFolderIds,
          activeFolder,
        }"
        @update:expanded-folder-ids="emit('update:expandedFolderIds', $event)"
      />
    </template>
  </ul>
</template>

<script setup lang="ts">
import type {
  FolderListing,
  NoteTopology,
  Folder,
} from "@generated/doughnut-backend-api"
import SidebarFolderItem from "./SidebarFolderItem.vue"
import SidebarNoteItem from "./SidebarNoteItem.vue"
import { sidebarStructuralRefreshKey } from "./sidebarStructuralRefresh"
import {
  buildUnsortedStructuralRows,
  sortSidebarStructuralRows,
  type SidebarStructuralRow,
} from "./sidebarStructuralSort"
import { computed, ref, watch } from "vue"
import { useNoteSidebarPeerSort } from "@/composables/useNoteSidebarPeerSort"
import { getCachedListing, setCachedListing } from "./sidebarFolderListingCache"
import { requestNotebookFolderListing } from "@/utils/notebookFolderListingRequest"

const { sortPeerSpec } = useNoteSidebarPeerSort()

function folderNumericId(folder: Folder): number | undefined {
  return folder.id
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
  /** When omitted (e.g. notebook overview with no `index` note), root notes still render without an active note in the tree */
  activeNoteTopology?: NoteTopology
  /** When set, list notes inside this folder. When omitted, list notebook root notes. */
  folderId?: number
  /** Notifies enclosing folder row (when nested) how many peers this listing renders. */
  onStructuralPeerCount?: (count: number) => void
  /** ARIA level for treeitem descendants. Defaults to 1 (root tree). */
  level?: number
  expandedFolderIds: Set<number>
  activePathFolderIds: Set<number>
  activeFolder?: Folder
}

const props = defineProps<Props>()

const emit = defineEmits<{
  "update:expandedFolderIds": [next: Set<number>]
}>()

const currentLevel = computed(() => props.level ?? 1)

const rawRows = ref<SidebarStructuralRow[]>([])

const displayRows = computed(() =>
  sortSidebarStructuralRows(rawRows.value, sortPeerSpec.value)
)

function applyListing(listing: FolderListing) {
  const noteTopologies = listing.noteTopologies ?? []
  rawRows.value = buildUnsortedStructuralRows(noteTopologies, listing.folders)
  props.onStructuralPeerCount?.(rawRows.value.length)
}

async function refreshListing() {
  try {
    const { data: listing, error } = await requestNotebookFolderListing(
      props.notebookId,
      props.folderId ?? null
    )
    if (error || !listing) throw new Error("Failed to load listing")
    applyListing(listing)
    setCachedListing(props.notebookId, props.folderId ?? null, listing)
  } catch {
    rawRows.value = []
    props.onStructuralPeerCount?.(0)
  }
}

watch(
  () => [props.notebookId, props.folderId] as const,
  ([notebookId, folderId]) => {
    const cached = getCachedListing(notebookId, folderId ?? null)
    if (cached) {
      applyListing(cached)
    }
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
