<template>
  <div class="w-full">
    <p v-if="loadError" class="text-error text-sm mb-2">
      {{ loadError }}
    </p>
    <div class="daisy-join w-full folder-selector-join">
      <div
        class="folder-selector-join-select daisy-join-item flex flex-1 min-w-0 items-center border border-base-content/20 bg-base-100 pl-3 pr-2 rounded-l-lg min-h-[2.75rem]"
      >
        <select
          v-model="selectModel"
          class="daisy-select daisy-select-sm w-full min-h-0 flex-1 border-0 bg-transparent shadow-none focus:outline-none focus:ring-0"
          data-testid="folder-move-parent-select"
          :disabled="disabled"
        >
          <optgroup label="Notebook root">
            <option value="__root__">Notebook root</option>
          </optgroup>
          <optgroup v-if="ancestorIds.length > 0" label="Ancestor folders">
            <option
              v-for="id in ancestorIds"
              :key="`a-${id}`"
              :value="String(id)"
            >
              {{ quickPathLabel(id) }}
            </option>
          </optgroup>
          <optgroup v-if="neighbourIds.length > 0" label="Neighbour folders">
            <option
              v-for="id in neighbourIds"
              :key="`n-${id}`"
              :value="String(id)"
            >
              {{ quickPathLabel(id) }}
            </option>
          </optgroup>
          <optgroup v-if="needsSyntheticOption && modelValue != null" label="Selected">
            <option :value="String(modelValue.id)">
              {{ selectionSummary }}
            </option>
          </optgroup>
        </select>
      </div>
      <div
        class="folder-selector-join-append daisy-join-item flex shrink-0 self-stretch items-stretch"
      >
        <button
          type="button"
          class="daisy-btn daisy-btn-outline daisy-btn-neutral rounded-l-none rounded-r-lg"
          :disabled="disabled"
          title="Search folders"
          aria-label="Search folders"
          data-testid="folder-selector-more-button"
          @click="searchOpen = true"
        >
          <MoreHorizontal class="w-5 h-5" />
        </button>
      </div>
    </div>
    <Modal v-if="searchOpen" align-top @close_request="searchOpen = false">
      <template #body>
        <FolderSearchForm
          :notebook-id="notebookId"
          :context-folder-id="searchContextFolderId"
          :current-path-display="selectionSummary"
          @close="searchOpen = false"
          @select="onSearchSelect"
          @index-loaded="onIndexLoaded"
        />
      </template>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import type { Folder } from "@generated/doughnut-backend-api"
import { MoreHorizontal } from "lucide-vue-next"
import { computed, onMounted, ref } from "vue"
import Modal from "@/components/commons/Modal.vue"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { requestNotebookFolderListing } from "@/utils/notebookFolderListingRequest"
import FolderSearchForm from "./FolderSearchForm.vue"
import {
  ancestorsFromChain,
  folderChainWithParentIds,
  folderPathLabel,
  folderRowsById,
} from "./folderSelectorUtils"

const props = defineProps<{
  notebookId: number
  /**
   * Folder used for quick-pick neighbours (organize = moving folder; new folder = default parent).
   * Null when the UI context is notebook root (no folder).
   */
  contextFolder: Folder | null
  /** Root-to-leaf ancestor chain from NoteRealm (may include the moving folder). */
  ancestorFolders: Folder[]
  /** `null` means notebook root. */
  modelValue: Folder | null
  /**
   * Display label for the current selection when its path cannot be resolved from the local index.
   * Used as a fallback for the synthetic dropdown option before the full folder index is loaded.
   */
  disabled?: boolean
}>()

const emit = defineEmits<{
  "update:modelValue": [value: Folder | null]
}>()

const loadError = ref<string | undefined>(undefined)
const searchOpen = ref(false)

// Rows loaded only for the search dialog (full index, lazy)
const searchIndexRows = ref<Folder[]>([])

// Neighbour folders loaded via one cheap listing call (full rows for v-model)
const neighbourFolders = ref<Folder[]>([])

const neighbourRows = computed((): Folder[] =>
  neighbourFolders.value.map((f) => ({
    ...f,
    parentFolderId: parentFolderId.value ?? undefined,
  }))
)

const contextFolderId = computed(() => props.contextFolder?.id ?? null)

const ancestorRows = computed(() => {
  if (contextFolderId.value == null) return []
  return ancestorsFromChain(contextFolderId.value, props.ancestorFolders)
    .ancestorFolders
})

const parentFolderId = computed(() => {
  if (contextFolderId.value == null) return null
  return ancestorsFromChain(contextFolderId.value, props.ancestorFolders)
    .parentFolderId
})

/** Omit when null so folder search does not exclude a subtree (new folder at root). */
const searchContextFolderId = computed(() => contextFolderId.value ?? undefined)

onMounted(async () => {
  const pid = parentFolderId.value
  try {
    const { data: listing, error } = await apiCallWithLoading(() =>
      requestNotebookFolderListing(props.notebookId, pid)
    )
    if (error || !listing)
      throw new Error("Failed to load neighbouring folders")
    neighbourFolders.value = (listing.folders ?? []).filter(
      (f) => f.id !== contextFolderId.value
    )
  } catch {
    loadError.value = "Failed to load neighbouring folders"
  }
})

/** Minimal byId map built from quick-pick data only. Used for path display in the dropdown. */
const quickPickById = computed(() => {
  const allAncestorRows = folderChainWithParentIds(props.ancestorFolders)
  return folderRowsById([...allAncestorRows, ...neighbourRows.value])
})

/** Full byId for display when we have the search index (after search dialog opened). */
const displayById = computed(() =>
  searchIndexRows.value.length > 0
    ? folderRowsById(searchIndexRows.value)
    : quickPickById.value
)

const ancestorIds = computed(() => ancestorRows.value.map((r) => r.id))
const neighbourIds = computed(() => neighbourRows.value.map((r) => r.id))

const quickPickIdSet = computed(() => {
  const s = new Set<number>()
  for (const id of ancestorIds.value) s.add(id)
  for (const id of neighbourIds.value) s.add(id)
  return s
})

const needsSyntheticOption = computed(
  () =>
    props.modelValue != null && !quickPickIdSet.value.has(props.modelValue.id)
)

const selectionSummary = computed(() => {
  if (props.modelValue == null) return "Notebook root"
  const id = props.modelValue.id
  const path = folderPathLabel(id, displayById.value)
  if (path) return path
  return props.modelValue.name
})

function quickPathLabel(id: number): string {
  return folderPathLabel(id, quickPickById.value)
}

function resolveFolderById(id: number): Folder | null {
  if (props.modelValue?.id === id) return props.modelValue
  const fromChain = props.ancestorFolders.find((f) => f.id === id)
  if (fromChain) return fromChain
  return neighbourFolders.value.find((f) => f.id === id) ?? null
}

const selectModel = computed({
  get(): string {
    if (props.modelValue == null) return "__root__"
    return String(props.modelValue.id)
  },
  set(v: string) {
    if (v === "__root__") emit("update:modelValue", null)
    else emit("update:modelValue", resolveFolderById(Number(v)))
  },
})

function onSearchSelect(row: Folder | null) {
  emit("update:modelValue", row)
  searchOpen.value = false
}

function onIndexLoaded(rows: Folder[]) {
  searchIndexRows.value = rows
}
</script>

<style scoped>
.folder-selector-join-append :deep(button) {
  height: 100%;
  min-height: 2.75rem;
}
</style>
