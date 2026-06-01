<template>
  <div
    data-note-sidebar-root
    class="flex flex-col flex-1 min-h-0 overflow-x-visible"
  >
    <SidebarToolbar
      v-if="!sidebarReadonly"
      :notebook-id="notebookId"
      :active-note-realm="activeNoteRealm"
      :active-folder-realm="activeFolderRealm"
      :breadcrumb-folders="breadcrumbFolders"
    />
    <div
      ref="treeScrollRef"
      class="sidebar-tree-scroll overflow-y-auto flex-1 min-h-0"
    >
      <SidebarNotebookTreeScrollportPathHint
        v-if="sidebarTreeShown && breadcrumbFolders.length > 0"
        :path-folders="breadcrumbFolders"
        :notebook-id="notebookId"
        :scroll-root="treeScrollRef"
      />
      <SidebarInner
        v-if="sidebarTreeShown"
        :notebook-id="notebookId"
        :active-note-topology="activeNoteTopology"
        :activeFolder="activeFolderRealm?.folder"
        :activePathFolderIds="activePathFolderIds"
        :expanded-folder-ids="expandedFolderIds"
        @update:expanded-folder-ids="onUpdateExpandedFolderIds"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Ref } from "vue"
import { computed, inject, ref, watch } from "vue"
import type {
  Folder,
  FolderRealm,
  NoteRealm,
  User,
} from "@generated/doughnut-backend-api"
import SidebarToolbar from "./SidebarToolbar.vue"
import SidebarInner from "./SidebarInner.vue"
import SidebarNotebookTreeScrollportPathHint from "./SidebarNotebookTreeScrollportPathHint.vue"
import { useSidebarCreationReadonly } from "@/composables/useSidebarCreationReadonly"

const props = withDefaults(
  defineProps<{
    /** When set, highlights the active note and expands its ancestors */
    activeNoteRealm?: NoteRealm
    /** Notebook id for sidebar chrome; toolbar shows root create until active note topology exists */
    notebookId: number
    /** When no active note realm (e.g. notebook overview); mirrors notebook realm readonly for toolbar */
    notebookReadonly?: boolean
    /** Set on folder page for active-folder toolbar/tree scope */
    activeFolderRealm?: FolderRealm
    breadcrumbFolders?: Folder[]
  }>(),
  { breadcrumbFolders: () => [] }
)

const treeScrollRef = ref<HTMLElement | null>(null)

const expandedFolderIds = ref<Set<number>>(new Set())

function onUpdateExpandedFolderIds(next: Set<number>) {
  expandedFolderIds.value = next
}

const activeNoteTopology = computed(
  () => props.activeNoteRealm?.note?.noteTopology
)

const activePathFolderIds = computed(
  () => new Set(props.breadcrumbFolders.map((seg) => seg.id))
)

watch(
  () => props.notebookId,
  (notebookId, previousNotebookId) => {
    if (previousNotebookId !== undefined && notebookId !== previousNotebookId) {
      expandedFolderIds.value = new Set()
    }
  }
)

function scrollSidebarTreeToTop() {
  const el = treeScrollRef.value
  if (el) el.scrollTop = 0
}

watch(
  () => props.activeNoteRealm != null || props.activeFolderRealm != null,
  (hasActiveContext, hadActiveContext) => {
    if (hadActiveContext && !hasActiveContext) {
      scrollSidebarTreeToTop()
    }
  }
)

const currentUser = inject<Ref<User | undefined>>("currentUser")
const sidebarReadonly = useSidebarCreationReadonly(currentUser, () => ({
  activeNoteRealm: props.activeNoteRealm,
  notebookReadonly: props.notebookReadonly,
}))

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
