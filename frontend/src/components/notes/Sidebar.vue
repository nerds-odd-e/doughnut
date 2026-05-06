<template>
  <div
    data-note-sidebar-root
    class="daisy-flex daisy-flex-col daisy-flex-1 daisy-min-h-0 daisy-overflow-x-visible"
  >
    <NoteSidebarToolbar
      v-if="!sidebarReadonly"
      :notebook-id="notebookId"
      :note="activeNoteRealm?.note"
      :active-note-topology-resolved="noteContextResolved"
      :resolved-create-parent-folder-id="resolvedCreateParentFolderId"
      :create-parent-location-description="createParentLocationDescription"
      :user-active-folder="userActiveFolder"
    />
    <div
      class="sidebar-tree-scroll daisy-overflow-y-auto daisy-flex-1 daisy-min-h-0"
    >
      <SidebarInner
        v-if="sidebarTreeShown"
        :key="notebookId"
        :notebook-id="notebookId"
        :active-note-topology="activeNoteTopology"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType, Ref } from "vue"
import { computed, inject, provide, ref, watch } from "vue"
import type { NoteRealm, User } from "@generated/doughnut-backend-api"
import NoteSidebarToolbar from "./NoteSidebarToolbar.vue"
import SidebarInner from "./SidebarInner.vue"
import {
  sidebarTreeKey,
  useNotebookRootCreateTarget,
  type SidebarUserActiveFolder,
} from "./useNoteSidebarTree"
import {
  notebookSidebarNotebookPageContext,
  notebookSidebarUserActiveFolder,
} from "@/composables/useCurrentNoteSidebarState"

const props = defineProps({
  /** When set, highlights the active note and expands its ancestors */
  activeNoteRealm: {
    type: Object as PropType<NoteRealm | undefined>,
    required: false,
  },
  /** Notebook id for sidebar chrome; toolbar shows root create until active note topology exists */
  notebookId: { type: Number, required: true },
})

const expandedFolderIds = ref<Set<number>>(new Set())
const userActiveFolder = ref<SidebarUserActiveFolder | null>(null)

function ensureFolderExpanded(folderId: number | undefined) {
  if (folderId == null) return
  if (expandedFolderIds.value.has(folderId)) return
  expandedFolderIds.value = new Set([...expandedFolderIds.value, folderId])
}

function toggleFolderId(folderId: number) {
  const next = new Set(expandedFolderIds.value)
  if (next.has(folderId)) {
    next.delete(folderId)
  } else {
    next.add(folderId)
  }
  expandedFolderIds.value = next
}

const activeNoteTopology = computed(
  () => props.activeNoteRealm?.note?.noteTopology
)

const activeNoteFolderIds = computed(() => {
  const ids = new Set<number>()
  const fid = activeNoteTopology.value?.folderId
  if (fid != null) {
    ids.add(fid)
  }
  return ids
})

const ancestorFolderIds = computed(() => {
  const ids = new Set<number>()
  for (const seg of props.activeNoteRealm?.ancestorFolders ?? []) {
    if (seg.id != null) {
      ids.add(seg.id)
    }
  }
  const fid = activeNoteTopology.value?.folderId
  if (fid != null) ids.add(fid)
  return ids
})

provide(sidebarTreeKey, {
  expandedFolderIds,
  toggleFolder: toggleFolderId,
  ancestorFolderIds,
  activeNoteFolderIds,
  userActiveFolder,
})

watch(
  activeNoteTopology,
  (topology) => {
    ensureFolderExpanded(topology?.folderId)
  },
  { immediate: true, deep: true }
)

watch(
  () => props.notebookId,
  (notebookId, previousNotebookId) => {
    if (previousNotebookId !== undefined && notebookId !== previousNotebookId) {
      expandedFolderIds.value = new Set()
      userActiveFolder.value = null
    }
  }
)

const currentUser = inject<Ref<User | undefined>>("currentUser")
const sidebarReadonly = computed(
  () =>
    !currentUser?.value ||
    notebookSidebarNotebookPageContext.value?.isNotebookReadOnly === true ||
    (props.activeNoteRealm != null && props.activeNoteRealm.fromBazaar === true)
)

const noteContextResolved = computed(() => activeNoteTopology.value != null)

const activeNoteRealmRef = computed(() => props.activeNoteRealm)

const { resolvedCreateParentFolderId, createParentLocationDescription } =
  useNotebookRootCreateTarget(
    userActiveFolder,
    activeNoteRealmRef,
    noteContextResolved
  )

watch(
  userActiveFolder,
  (v) => {
    notebookSidebarUserActiveFolder.value = v
  },
  { deep: true, immediate: true }
)

/** Notebook overview pages may load root notes without an anchor note (e.g. no index note). */
const sidebarTreeShown = computed(
  () => props.activeNoteRealm === undefined || activeNoteTopology.value != null
)
</script>

<style scoped lang="scss">
/* scrollIntoView respects scroll-padding on the scrollport */
.sidebar-tree-scroll {
  scroll-padding-top: 0.75rem;
  scroll-padding-bottom: 0.5rem;
}
</style>
