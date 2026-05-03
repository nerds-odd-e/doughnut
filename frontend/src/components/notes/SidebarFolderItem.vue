<template>
  <li
    v-if="folderId != null"
    class="sidebar-folder-li"
    role="treeitem"
    tabindex="0"
    :aria-level="currentLevel"
    :aria-expanded="isExpanded"
    :aria-label="folder.name"
    :class="{
      'active-item': isNotePathFolder,
      'sidebar-folder-user-active': isUserActiveFolder,
    }"
    @focusout="onFolderRowFocusOut"
  >
    <div class="folder-row" @click="toggleExpand">
      <button
        class="chevron-btn"
        aria-label="expand children"
        tabindex="-1"
        @click.stop="toggleExpand"
      >
        <ChevronDown v-if="isExpanded" :size="14" />
        <ChevronRight v-else :size="14" />
      </button>
      <span class="sidebar-folder-label">
        <FolderOpen v-if="isExpanded" :size="14" class="daisy-shrink-0 daisy-opacity-70" />
        <Folder v-else :size="14" class="daisy-shrink-0 daisy-opacity-70" />
        {{ folder.name }}
      </span>
      <span v-if="structuralChildCount != null" class="child-count">{{
        structuralChildCount
      }}</span>
    </div>
    <div v-if="isExpanded" class="folder-children">
      <SidebarInner
        v-bind="{
          notebookId,
          folderId,
          activeNoteTopology,
          onStructuralPeerCount: setStructuralChildCount,
          level: currentLevel + 1,
        }"
        :key="`folder-${folderId}`"
      />
    </div>
  </li>
</template>

<script setup lang="ts">
import type {
  NoteTopology,
  FolderTrailSegment,
} from "@generated/doughnut-backend-api"
import { ChevronDown, ChevronRight, Folder, FolderOpen } from "lucide-vue-next"
import SidebarInner from "./SidebarInner.vue"
import { sidebarTreeKey } from "./useNoteSidebarTree"
import { computed, inject, ref, watch } from "vue"

const props = defineProps<{
  folder: FolderTrailSegment
  notebookId: number
  activeNoteTopology?: NoteTopology
  level?: number
}>()

const currentLevel = computed(() => props.level ?? 1)

const tree = inject(sidebarTreeKey)!
const {
  expandedFolderIds,
  toggleFolder: toggleFolderId,
  ancestorFolderIds,
  activeNoteFolderIds,
  activeNoteTitle,
  userActiveFolderId,
} = tree

const structuralChildCount = ref<number | undefined>(undefined)

const folderId = computed(() => {
  const raw = props.folder.id
  if (raw == null || raw === "") return undefined
  return Number(raw)
})

function ensureFolderExpandedById(id: number | undefined) {
  if (id == null) return
  if (expandedFolderIds.value.has(id)) return
  expandedFolderIds.value = new Set([...expandedFolderIds.value, id])
}

watch(
  () =>
    [
      ancestorFolderIds.value,
      activeNoteTitle.value,
      props.folder.name,
      folderId.value,
    ] as const,
  () => {
    if (folderId.value == null) return
    if (
      ancestorFolderIds.value.has(folderId.value) ||
      (activeNoteTitle.value != null &&
        props.folder.name === activeNoteTitle.value)
    ) {
      ensureFolderExpandedById(folderId.value)
    }
  },
  { immediate: true }
)

const isExpanded = computed(
  () => folderId.value != null && expandedFolderIds.value.has(folderId.value)
)

const isNotePathFolder = computed(
  () =>
    folderId.value != null &&
    (activeNoteFolderIds.value.has(folderId.value) ||
      ancestorFolderIds.value.has(folderId.value))
)

const isUserActiveFolder = computed(
  () =>
    userActiveFolderId != null &&
    folderId.value != null &&
    userActiveFolderId.value === folderId.value
)

function setStructuralChildCount(count: number) {
  structuralChildCount.value = count
}

function onFolderRowFocusOut(event: FocusEvent) {
  if (
    userActiveFolderId == null ||
    folderId.value == null ||
    userActiveFolderId.value !== folderId.value
  ) {
    return
  }
  const li = event.currentTarget as HTMLElement
  const next = event.relatedTarget as Node | null
  if (next && li.contains(next)) return
  const root = li.closest("[data-note-sidebar-root]")
  if (
    next instanceof Element &&
    root != null &&
    root.contains(next) &&
    next.closest("[data-note-sidebar-toolbar]")
  ) {
    return
  }
  userActiveFolderId.value = null
}

function toggleExpand() {
  if (userActiveFolderId != null && folderId.value != null) {
    userActiveFolderId.value = folderId.value
  }
  if (folderId.value != null) {
    toggleFolderId(folderId.value)
  }
}
</script>

<style lang="scss" scoped>
.sidebar-folder-li {
  list-style: none;
}

.folder-row {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  min-height: 2rem;
  padding: 0.125rem 0.25rem 0.125rem 0;
  cursor: pointer;
  border-radius: 0.25rem;

  &:hover {
    background-color: var(--color-base-300, rgba(0, 0, 0, 0.08));
  }
}

.chevron-btn {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  padding: 0.125rem;
  border: none;
  background: transparent;
  color: currentColor;
  opacity: 0.6;
  cursor: pointer;
  border-radius: 0.2rem;

  &:hover {
    opacity: 1;
  }
}

.sidebar-folder-label {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex: 1;
  font-size: 0.875rem;
  font-weight: 500;
}

.child-count {
  font-size: 0.7rem;
  opacity: 0;
  padding: 0 0.25rem;
  transition: opacity 0.15s;

  .folder-row:hover & {
    opacity: 0.5;
  }
}

.folder-children {
  border-left: 1px solid var(--color-base-300, rgba(0, 0, 0, 0.12));
  margin-left: 0.75rem;
}

.active-item > .folder-row {
  background-color: var(--color-base-300, rgba(0, 0, 0, 0.08));
}

.sidebar-folder-user-active > .folder-row {
  background-color: color-mix(in srgb, var(--color-base-200, #f0f0f0) 75%, #3b82f6) !important;
  box-shadow: inset 2px 0 0 #3b82f6;
}
</style>
