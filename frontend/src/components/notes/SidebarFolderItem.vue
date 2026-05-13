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
      'active-item': isOnActivePath,
      'sidebar-folder-active': isActiveFolderRow,
    }"
  >
    <ScrollTo v-if="isActiveFolderRow" />
    <div class="folder-row" :data-sidebar-folder-row="String(folderId)">
      <button
        class="chevron-btn"
        aria-label="expand children"
        tabindex="-1"
        @click.stop="toggleExpand"
      >
        <ChevronRight
          :size="14"
          class="chevron-icon"
          :class="{ 'chevron-icon--open': isExpanded }"
          aria-hidden="true"
        />
      </button>
      <div class="folder-label-area" @click="toggleExpand">
        <router-link
          class="sidebar-folder-label"
          data-testid="sidebar-folder-open-page-link"
          :to="{
            name: 'folderPage',
            params: {
              notebookId: String(notebookId),
              folderId: String(folderId),
            },
          }"
        >
          {{ folder.name }}
        </router-link>
      </div>
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
          expandedFolderIds,
          activePathFolderIds,
          activeFolder,
        }"
        :key="`folder-${folderId}`"
        @update:expanded-folder-ids="emit('update:expandedFolderIds', $event)"
      />
    </div>
  </li>
</template>

<script setup lang="ts">
import type { Folder, NoteTopology } from "@generated/doughnut-backend-api"
import { ChevronRight } from "lucide-vue-next"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import SidebarInner from "./SidebarInner.vue"
import { computed, ref, watch } from "vue"

const props = defineProps<{
  folder: Folder
  notebookId: number
  activeNoteTopology?: NoteTopology
  level?: number
  expandedFolderIds: Set<number>
  activePathFolderIds: Set<number>
  activeFolder?: Folder
}>()

const emit = defineEmits<{
  "update:expandedFolderIds": [next: Set<number>]
}>()

const currentLevel = computed(() => props.level ?? 1)

const structuralChildCount = ref<number | undefined>(undefined)

const folderId = computed(() => props.folder.id)

function ensureFolderExpandedById(id: number | undefined) {
  if (id == null) return
  if (props.expandedFolderIds.has(id)) return
  emit("update:expandedFolderIds", new Set([...props.expandedFolderIds, id]))
}

watch(
  () =>
    [
      props.activePathFolderIds,
      folderId.value,
      props.activeFolder?.id,
    ] as const,
  () => {
    if (folderId.value == null) return
    if (props.activeFolder?.id === folderId.value) {
      return
    }
    if (props.activePathFolderIds.has(folderId.value)) {
      ensureFolderExpandedById(folderId.value)
    }
  },
  { immediate: true }
)

const isExpanded = computed(
  () => folderId.value != null && props.expandedFolderIds.has(folderId.value)
)

const isOnActivePath = computed(
  () => folderId.value != null && props.activePathFolderIds.has(folderId.value)
)

const isActiveFolderRow = computed(
  () => folderId.value != null && props.activeFolder?.id === folderId.value
)

function setStructuralChildCount(count: number) {
  structuralChildCount.value = count
}

function toggleExpand() {
  if (folderId.value == null) return
  const next = new Set(props.expandedFolderIds)
  if (next.has(folderId.value)) {
    next.delete(folderId.value)
  } else {
    next.add(folderId.value)
  }
  emit("update:expandedFolderIds", next)
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
  border-radius: 0.25rem;

  &:hover {
    background-color: var(--fallback-b3, oklch(var(--b3) / 1));
  }
}

.chevron-btn {
  display: flex;
  align-items: center;
  justify-content: center;
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

.chevron-icon {
  flex-shrink: 0;
  transform: rotate(0deg);
  transform-origin: center;
  transition: transform 0.2s ease;

  &--open {
    transform: rotate(90deg);
  }
}

.folder-label-area {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}

.sidebar-folder-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  width: 100%;
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.sidebar-folder-label:hover {
  text-decoration: underline;
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
  background-color: var(--fallback-b3, oklch(var(--b3) / 1));
}

.sidebar-folder-active > .folder-row {
  color: var(--fallback-bc, oklch(var(--bc) / 1));
  background-color: color-mix(
    in oklch,
    var(--fallback-b2, oklch(var(--b2) / 1)) 78%,
    var(--fallback-p, oklch(var(--p) / 1)) 22%
  ) !important;
  box-shadow: inset 2px 0 0 var(--fallback-p, oklch(var(--p) / 1));
}
</style>
