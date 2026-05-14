<template>
  <div class="daisy-w-full" data-testid="folder-selector-search-dialog">
    <h3 class="daisy-font-bold daisy-text-lg daisy-mb-2">Find folder</h3>
    <p class="daisy-text-sm daisy-text-base-content/70 daisy-mb-1">
      Current selection
    </p>
    <p
      class="daisy-text-sm daisy-mb-4 daisy-break-words daisy-whitespace-normal daisy-max-w-full"
      data-testid="folder-selector-search-current-path"
    >
      {{ currentPathDisplay }}
    </p>
    <p v-if="indexLoadError" class="daisy-text-error daisy-text-sm daisy-mb-2">
      {{ indexLoadError }}
    </p>
    <div
      class="daisy-flex daisy-items-center daisy-gap-2 daisy-w-full daisy-mb-3 daisy-input daisy-input-bordered"
    >
      <Search class="daisy-w-4 daisy-h-4 daisy-shrink-0 daisy-opacity-50" />
      <input
        v-model="query"
        v-focus
        type="search"
        class="daisy-grow daisy-min-w-0 daisy-border-0 daisy-bg-transparent daisy-outline-none"
        placeholder="Search by name or path"
        data-testid="folder-selector-search-input"
        autocomplete="off"
      />
    </div>
    <ul
      class="daisy-menu daisy-flex-nowrap daisy-w-full daisy-bg-base-200 daisy-rounded-box daisy-max-h-64 daisy-overflow-y-auto"
    >
      <li v-if="showRootRow" class="daisy-w-full">
        <button
          type="button"
          class="daisy-text-left daisy-w-full daisy-break-words daisy-whitespace-normal"
          data-testid="folder-selector-search-result"
          data-folder-id="__root__"
          @click="pickRow(null)"
        >
          Notebook root
        </button>
      </li>
      <li v-for="r in filteredFolders" :key="r.id" class="daisy-w-full">
        <button
          type="button"
          class="daisy-text-left daisy-w-full daisy-break-words daisy-whitespace-normal"
          data-testid="folder-selector-search-result"
          :data-folder-id="String(r.id)"
          @click="pickRow(r)"
        >
          {{ rowDisplay(r.id) }}
        </button>
      </li>
    </ul>
    <div class="daisy-flex daisy-w-full daisy-justify-end daisy-mt-4">
      <button
        type="button"
        class="daisy-btn"
        data-testid="folder-selector-search-cancel"
        @click="close"
      >
        Cancel
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Folder } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { Search } from "lucide-vue-next"
import { computed, onMounted, ref } from "vue"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"
import {
  collectSubtreeFolderIds,
  folderPathLabel,
  folderRowsById,
} from "./folderSelectorUtils"

const props = defineProps<{
  notebookId: number
  /**
   * When set, search results exclude this folder and its descendants (move dialog).
   * Omit or null for pickers where no subtree should be excluded (e.g. new folder).
   */
  contextFolderId?: number | null
  /** Line-wrapped summary for the current selection */
  currentPathDisplay: string
}>()

const emit = defineEmits<{
  close: []
  select: [row: Folder | null]
  /** Emit loaded rows so caller can use them for path display after selection */
  indexLoaded: [rows: Folder[]]
}>()

const indexRows = ref<Folder[]>([])
const indexLoadError = ref<string | undefined>(undefined)
const query = ref("")

onMounted(async () => {
  query.value = ""
  indexLoadError.value = undefined
  try {
    const { data, error } = await apiCallWithLoading(() =>
      NotebookController.listNotebookFolderIndex({
        path: { notebook: props.notebookId },
      })
    )
    if (error || !data) throw new Error("Failed to load folders")
    indexRows.value = data
    emit("indexLoaded", indexRows.value)
  } catch (e: unknown) {
    indexLoadError.value = toOpenApiError(e).message ?? "Failed to load folders"
  }
})

const excludedFolderIds = computed(() => {
  if (props.contextFolderId == null) return new Set<number>()
  return collectSubtreeFolderIds(props.contextFolderId, indexRows.value)
})

const byId = computed(() => folderRowsById(indexRows.value))
const q = computed(() => query.value.trim().toLowerCase())

const showRootRow = computed(() => {
  if (!q.value) return true
  return "notebook root".includes(q.value) || q.value === "root"
})

const filteredFolders = computed(() => {
  const out: Folder[] = []
  for (const r of indexRows.value) {
    if (excludedFolderIds.value.has(r.id)) continue
    const path = folderPathLabel(r.id, byId.value).toLowerCase()
    const name = r.name.toLowerCase()
    if (!q.value || path.includes(q.value) || name.includes(q.value)) {
      out.push(r)
    }
  }
  const withPaths = out.map((r) => ({
    row: r,
    path: folderPathLabel(r.id, byId.value),
  }))
  withPaths.sort((a, b) => {
    const byLen = a.path.length - b.path.length
    if (byLen !== 0) return byLen
    return a.path.localeCompare(b.path)
  })
  return withPaths.map((x) => x.row)
})

function rowDisplay(id: number): string {
  return folderPathLabel(id, byId.value)
}

function pickRow(row: Folder | null) {
  emit("select", row)
  emit("close")
}

function close() {
  emit("close")
}
</script>
