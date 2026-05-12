<template>
  <div
    data-note-sidebar-root
    class="daisy-flex daisy-flex-col daisy-flex-1 daisy-min-h-0 daisy-overflow-x-visible"
  >
    <SidebarToolbar
      v-if="!sidebarReadonly"
      :notebook-id="notebookId"
      :active-note-realm="activeNoteRealm"
      :active-folder-realm="activeFolderRealm"
      :breadcrumb-folders="breadcrumbFolders"
    />
    <div
      class="sidebar-tree-scroll daisy-overflow-y-auto daisy-flex-1 daisy-min-h-0"
    >
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
  NotebookRealm,
  User,
} from "@generated/doughnut-backend-api"
import SidebarToolbar from "./SidebarToolbar.vue"
import SidebarInner from "./SidebarInner.vue"

const props = withDefaults(
  defineProps<{
    /** When set, highlights the active note and expands its ancestors */
    activeNoteRealm?: NoteRealm
    /** Notebook id for sidebar chrome; toolbar shows root create until active note topology exists */
    notebookId: number
    /** Layout chrome before a note realm exists (e.g. notebook overview); used for readonly when no active note */
    notebookRealm?: NotebookRealm
    /** Set on folder page for active-folder toolbar/tree scope */
    activeFolderRealm?: FolderRealm
    breadcrumbFolders?: Folder[]
  }>(),
  { breadcrumbFolders: () => [] }
)

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

const currentUser = inject<Ref<User | undefined>>("currentUser")
const sidebarReadonly = computed(() => {
  if (!currentUser?.value) return true
  const realmReadonly = props.activeNoteRealm?.notebookRealm.readonly
  if (realmReadonly === true) return true
  if (props.activeNoteRealm != null) return false
  return props.notebookRealm?.readonly === true
})

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
