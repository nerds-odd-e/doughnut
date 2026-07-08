<template>
  <div class="w-full">
    <p v-if="loadError" class="text-error text-sm mb-2">
      {{ loadError }}
    </p>
    <div class="daisy-join w-full">
      <select
        v-model="selectModel"
        class="daisy-select daisy-select-sm daisy-join-item min-h-[2.75rem] w-full min-w-0 flex-1"
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
      <button
        type="button"
        :class="FIELD_JOIN_APPEND_BUTTON_CLASS"
        :disabled="disabled"
        title="Search folders"
        aria-label="Search folders"
        data-testid="folder-selector-more-button"
        @click="onSearchFoldersClick"
      >
        <MoreHorizontal class="w-5 h-5" />
      </button>
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
import type { Folder } from "@generated/doughnut-backend-api"
import { MoreHorizontal } from "@lucide/vue"
import { computed, ref, toRef, watch } from "vue"
import Modal from "@/components/commons/Modal.vue"
import { useFolderSelectorNeighbourListing } from "@/composables/useFolderSelectorNeighbourListing"
import FolderSearchForm from "./FolderSearchForm.vue"
import { primeSoftKeyboard } from "@/utils/focusTarget"
import { FIELD_JOIN_APPEND_BUTTON_CLASS } from "@/utils/fieldJoinAppendButtonClass"
import {
  ancestorsFromChain,
  folderChainWithParentIds,
  folderPathLabel,
  folderRowsById,
  siblingFoldersForQuickPick,
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

const searchOpen = ref(false)

// Rows loaded only for the search dialog (full index, lazy)
const searchIndexRows = ref<Folder[]>([])

const contextFolderId = computed(() => props.contextFolder?.id ?? null)

const contextChain = computed(() => {
  if (contextFolderId.value == null) return null
  return ancestorsFromChain(contextFolderId.value, props.ancestorFolders)
})

const ancestorRows = computed(() => contextChain.value?.ancestorFolders ?? [])

const parentFolderId = computed(
  () => contextChain.value?.parentFolderId ?? null
)

/** Omit when null so folder search does not exclude a subtree (new folder at root). */
const searchContextFolderId = computed(() => contextFolderId.value ?? undefined)

const notebookIdRef = toRef(props, "notebookId")
const { neighbourFolders, loadError, loadNeighbourFolders } =
  useFolderSelectorNeighbourListing(notebookIdRef, parentFolderId)

const neighbourRows = computed(() =>
  siblingFoldersForQuickPick(
    neighbourFolders.value.filter((f) => f.id !== contextFolderId.value),
    parentFolderId.value
  )
)

watch(
  parentFolderId,
  () => {
    loadNeighbourFolders()
  },
  { immediate: true }
)

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

function onSearchFoldersClick() {
  primeSoftKeyboard()
  searchOpen.value = true
}

function onSearchSelect(row: Folder | null) {
  emit("update:modelValue", row)
  searchOpen.value = false
}

function onIndexLoaded(rows: Folder[]) {
  searchIndexRows.value = rows
}
</script>

