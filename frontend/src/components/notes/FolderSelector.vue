<template>
  <div class="daisy-w-full">
    <p v-if="loadError" class="daisy-text-error daisy-text-sm daisy-mb-2">
      {{ loadError }}
    </p>
    <div class="daisy-join daisy-w-full folder-selector-join">
      <div
        class="folder-selector-join-select daisy-join-item daisy-flex daisy-flex-1 daisy-min-w-0 daisy-items-center daisy-border daisy-border-base-content/20 daisy-bg-base-100 daisy-pl-3 daisy-pr-2 daisy-rounded-l-lg daisy-min-h-[2.75rem]"
      >
        <select
          v-model="selectModel"
          class="daisy-select daisy-select-sm daisy-w-full daisy-min-h-0 daisy-flex-1 daisy-border-0 daisy-bg-transparent daisy-shadow-none focus:daisy-outline-none focus:daisy-ring-0"
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
            <option :value="String(modelValue)">
              {{ selectionSummary }}
            </option>
          </optgroup>
        </select>
      </div>
      <div
        class="folder-selector-join-append daisy-join-item daisy-flex daisy-shrink-0 daisy-self-stretch daisy-items-stretch"
      >
        <button
          type="button"
          class="daisy-btn daisy-btn-outline daisy-btn-neutral daisy-rounded-l-none daisy-rounded-r-lg"
          :disabled="disabled"
          title="Search folders"
          aria-label="Search folders"
          data-testid="folder-selector-more-button"
          @click="searchOpen = true"
        >
          <MoreHorizontal class="daisy-w-5 daisy-h-5" />
        </button>
      </div>
    </div>
    <Modal v-if="searchOpen" @close_request="searchOpen = false">
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
import type {
  Folder,
  NotebookFolderIndexRow,
} from "@generated/doughnut-backend-api"
import { MoreHorizontal } from "lucide-vue-next"
import { computed, onMounted, ref } from "vue"
import Modal from "@/components/commons/Modal.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import FolderSearchForm from "./FolderSearchForm.vue"
import {
  ancestorsFromChain,
  folderChainToIndexRows,
  folderPathLabel,
  folderRowsById,
} from "./folderSelectorUtils"

const props = defineProps<{
  notebookId: number
  /**
   * Folder used for quick-pick neighbours (organize = moving folder; new folder = default parent).
   * Null when the UI context is notebook root (no folder id).
   */
  contextFolderId: number | null
  /** Root-to-leaf ancestor chain from NoteRealm (may include the moving folder). */
  ancestorFolders: Folder[]
  modelValue: number | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  "update:modelValue": [value: number | null]
}>()

const storageAccessor = useStorageAccessor()
const loadError = ref<string | undefined>(undefined)
const searchOpen = ref(false)

// Rows loaded only for the search dialog (full index, lazy)
const searchIndexRows = ref<NotebookFolderIndexRow[]>([])

// Neighbour folders loaded via one cheap listing call
const neighbourRows = ref<NotebookFolderIndexRow[]>([])

const ancestorRows = computed(() => {
  if (props.contextFolderId == null) return []
  return ancestorsFromChain(props.contextFolderId, props.ancestorFolders)
    .ancestorRows
})

const parentFolderId = computed(() => {
  if (props.contextFolderId == null) return null
  return ancestorsFromChain(props.contextFolderId, props.ancestorFolders)
    .parentFolderId
})

/** Omit when null so folder search does not exclude a subtree (new folder at root). */
const searchContextFolderId = computed(() => props.contextFolderId ?? undefined)

onMounted(async () => {
  const pid = parentFolderId.value
  try {
    const api = storageAccessor.value.storedApi()
    if (pid == null) {
      // Moving folder is at notebook root: load root-level folders for neighbours
      const listing = await api.loadNotebookRootNotes(props.notebookId)
      neighbourRows.value = (listing.folders ?? [])
        .filter((f) => f.id !== props.contextFolderId)
        .map((f) => ({
          id: f.id,
          name: f.name,
          parentFolderId: undefined,
        }))
    } else {
      const listing = await api.loadFolderListing(props.notebookId, pid)
      neighbourRows.value = (listing.folders ?? [])
        .filter((f) => f.id !== props.contextFolderId)
        .map((f) => ({
          id: f.id,
          name: f.name,
          parentFolderId: pid,
        }))
    }
  } catch {
    loadError.value = "Failed to load neighbouring folders"
  }
})

/** Minimal byId map built from quick-pick data only. Used for path display in the dropdown. */
const quickPickById = computed(() => {
  const allAncestorRows = folderChainToIndexRows(props.ancestorFolders)
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
  () => props.modelValue != null && !quickPickIdSet.value.has(props.modelValue)
)

const selectionSummary = computed(() => {
  if (props.modelValue == null) return "Notebook root"
  return folderPathLabel(props.modelValue, displayById.value)
})

function quickPathLabel(id: number): string {
  return folderPathLabel(id, quickPickById.value)
}

const selectModel = computed({
  get(): string {
    if (props.modelValue == null) return "__root__"
    return String(props.modelValue)
  },
  set(v: string) {
    if (v === "__root__") emit("update:modelValue", null)
    else emit("update:modelValue", Number(v))
  },
})

function onSearchSelect(folderId: number | null) {
  emit("update:modelValue", folderId)
  searchOpen.value = false
}

function onIndexLoaded(rows: NotebookFolderIndexRow[]) {
  searchIndexRows.value = rows
}
</script>

<style scoped>
.folder-selector-join-append :deep(button) {
  height: 100%;
  min-height: 2.75rem;
}
</style>
