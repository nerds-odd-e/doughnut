<template>
  <div
    data-note-sidebar-root
    class="daisy-flex daisy-flex-col daisy-flex-1 daisy-min-h-0 daisy-overflow-x-visible"
  >
    <NoteSidebarToolbar
      v-if="!sidebarReadonly"
      :notebook-id="notebookId"
      :note="activeNoteRealm?.note"
      :resolved-create-parent-folder="resolvedCreateParentFolder"
      :create-parent-location-description="createParentLocationDescription"
      :active-folder="notebookSidebarActiveFolder"
      :ancestor-folders="activeNoteRealm?.ancestorFolders ?? []"
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
} from "./useNoteSidebarTree"
import {
  notebookSidebarNotebookClientView,
  notebookSidebarActiveFolder,
  folderPageBreadcrumbFolders,
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

const activeNoteTopology = computed(
  () => props.activeNoteRealm?.note?.noteTopology
)

const activePathFolderIds = computed(() => {
  const ids = new Set<number>()
  if (notebookSidebarActiveFolder.value != null) {
    for (const seg of folderPageBreadcrumbFolders.value) {
      if (seg.id != null) ids.add(seg.id)
    }
    return ids
  }
  for (const seg of props.activeNoteRealm?.ancestorFolders ?? []) {
    if (seg.id != null) ids.add(seg.id)
  }
  return ids
})

provide(sidebarTreeKey, {
  expandedFolderIds,
  activePathFolderIds,
  activeFolder: notebookSidebarActiveFolder,
})

watch(
  () => props.notebookId,
  (notebookId, previousNotebookId) => {
    if (previousNotebookId !== undefined && notebookId !== previousNotebookId) {
      expandedFolderIds.value = new Set()
      notebookSidebarActiveFolder.value = null
    }
  }
)

const currentUser = inject<Ref<User | undefined>>("currentUser")
const sidebarReadonly = computed(() => {
  if (!currentUser?.value) return true
  const realmReadonly = props.activeNoteRealm?.notebookView.readonly
  if (realmReadonly === true) return true
  if (props.activeNoteRealm != null) return false
  return notebookSidebarNotebookClientView.value?.readonly === true
})

const noteContextResolved = computed(() => activeNoteTopology.value != null)

const activeNoteRealmRef = computed(() => props.activeNoteRealm)

const { resolvedCreateParentFolder, createParentLocationDescription } =
  useNotebookRootCreateTarget(
    notebookSidebarActiveFolder,
    activeNoteRealmRef,
    noteContextResolved
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
