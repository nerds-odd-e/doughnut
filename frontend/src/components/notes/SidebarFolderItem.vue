<template>
  <li
    v-if="folderId != null"
    class="daisy-list-group-item daisy-list-group-item-action daisy-py-2 daisy-pb-0 daisy-pe-0 daisy-border-0"
    :data-sidebar-folder-expanded="isExpanded"
    :class="{
      'active-item': isActiveFolder,
    }"
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
        activeNoteRealm,
        onStructuralPeerCount: setStructuralChildCount,
      }"
      :key="`folder-${folderId}`"
    />
  </li>
</template>

<script setup lang="ts">
import type {
  NoteRealm,
  NotebookRootFolder,
} from "@generated/doughnut-backend-api"
import SidebarInner from "./SidebarInner.vue"
import {
  sidebarActiveNoteFolderSlugPrefixesKey,
  sidebarExpandedFolderSlugsKey,
  sidebarToggleFolderSlugKey,
} from "./sidebarFolderExpansion"
import { computed, inject, ref } from "vue"

const props = defineProps<{
  folder: NotebookRootFolder
  notebookId: number
  activeNoteRealm?: NoteRealm
}>()

const expandedFolderSlugs = inject(sidebarExpandedFolderSlugsKey)!
const toggleFolderSlug = inject(sidebarToggleFolderSlugKey)!
const activeNoteFolderSlugPrefixes = inject(
  sidebarActiveNoteFolderSlugPrefixesKey
)!

const structuralChildCount = ref<number | undefined>(undefined)

const folderId = computed(() => {
  const raw = props.folder.id
  if (raw == null || raw === "") return undefined
  return Number(raw)
})

const slug = computed(() => props.folder.slug ?? "")

const isExpanded = computed(() => expandedFolderSlugs.value.has(slug.value))

const isActiveFolder = computed(
  () =>
    slug.value.length > 0 && activeNoteFolderSlugPrefixes.value.has(slug.value)
)

function setStructuralChildCount(count: number) {
  structuralChildCount.value = count
}

function toggleExpand() {
  toggleFolderSlug(slug.value)
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
</style>
