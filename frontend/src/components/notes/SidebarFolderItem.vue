<template>
  <li
    v-if="folderId != null"
    class="daisy-list-group-item daisy-list-group-item-action daisy-py-2 daisy-pb-0 daisy-pe-0 daisy-border-0"
    tabindex="0"
    :data-sidebar-folder-expanded="isExpanded"
    :class="{
      'active-item': isNotePathFolder,
      'sidebar-folder-user-active': isUserActiveFolder,
    }"
    @focusout="onFolderRowFocusOut"
  >
    <div
      class="daisy-flex daisy-w-full daisy-justify-between daisy-items-start folder-content"
      @click="toggleExpand"
    >
      <span class="sidebar-folder-label daisy-cursor-default">{{
        folder.name
      }}</span>
      <span
        role="button"
        title="expand children"
        class="daisy-badge daisy-cursor-pointer"
        @click.stop="toggleExpand"
        >{{ structuralChildCount ?? "..." }}</span
      >
    </div>
    <SidebarInner
      v-if="isExpanded"
      v-bind="{
        notebookId,
        folderId,
        activeNoteTopology,
        onStructuralPeerCount: setStructuralChildCount,
      }"
      :key="`folder-${folderId}`"
    />
  </li>
</template>

<script setup lang="ts">
import type {
  NoteTopology,
  NotebookRootFolder,
} from "@generated/doughnut-backend-api"
import SidebarInner from "./SidebarInner.vue"
import {
  sidebarStructuralSidebarTitlesKey,
  sidebarActiveNoteFolderIdsKey,
  sidebarExpandedFolderIdsKey,
  sidebarToggleFolderIdKey,
  sidebarUserActiveFolderIdKey,
} from "./sidebarFolderExpansion"
import { computed, inject, ref, watch } from "vue"

const props = defineProps<{
  folder: NotebookRootFolder
  notebookId: number
  activeNoteTopology?: NoteTopology
}>()

const expandedFolderIds = inject(sidebarExpandedFolderIdsKey)!
const toggleFolderId = inject(sidebarToggleFolderIdKey)!
const structuralTitles = inject(sidebarStructuralSidebarTitlesKey)!
const activeNoteFolderIds = inject(sidebarActiveNoteFolderIdsKey)!
const userActiveFolderId = inject(sidebarUserActiveFolderIdKey, undefined)

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
  () => [structuralTitles.value, props.folder.name, folderId.value] as const,
  () => {
    if (folderId.value == null) return
    if (structuralTitles.value.has(props.folder.name)) {
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
    (folderId.value != null && activeNoteFolderIds.value.has(folderId.value)) ||
    structuralTitles.value.has(props.folder.name)
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
.active-item {
  border-left: 1px solid gray !important;
}

.folder-content {
  position: relative;
  padding-bottom: 4px;
}

.sidebar-folder-user-active {
  background-color: color-mix(in srgb, var(--color-base-200, #f0f0f0) 75%, #3b82f6) !important;
  box-shadow: inset 2px 0 0 #3b82f6;
}
</style>
