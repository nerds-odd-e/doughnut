<template>
  <div class="daisy-ml-[-1rem]" data-note-sidebar-root>
    <NoteSidebarToolbar
      v-if="!sidebarReadonly"
      :notebook-id="notebookId"
      :note="activeNoteRealm?.note"
      :active-note-topology-resolved="noteContextResolved"
      :user-active-folder-id="userActiveFolderId"
    />
    <SidebarInner
      v-if="sidebarTreeShown"
      :key="notebookId"
      :notebook-id="notebookId"
      :active-note-realm="activeNoteRealm"
    />
  </div>
</template>

<script setup lang="ts">
import type { PropType, Ref } from "vue"
import { computed, inject, provide, ref, watch } from "vue"
import type { NoteRealm, User } from "@generated/doughnut-backend-api"
import NoteSidebarToolbar from "./NoteSidebarToolbar.vue"
import SidebarInner from "./SidebarInner.vue"
import {
  sidebarStructuralSidebarTitlesKey,
  sidebarActiveNoteFolderIdsKey,
  sidebarExpandedFolderIdsKey,
  sidebarToggleFolderIdKey,
  sidebarUserActiveFolderIdKey,
} from "./sidebarFolderExpansion"
import { notebookSidebarNotebookPageContext } from "@/composables/useCurrentNoteSidebarState"
import { structuralSidebarTitlesFromRealm } from "./sidebarStructuralTitles"

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
const userActiveFolderId = ref<number | null>(null)

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

const structuralSidebarTitles = computed(() =>
  structuralSidebarTitlesFromRealm(props.activeNoteRealm)
)

const activeNoteFolderIds = computed(() => {
  const ids = new Set<number>()
  const fid = props.activeNoteRealm?.note?.noteTopology?.folderId
  if (fid != null) {
    ids.add(fid)
  }
  return ids
})

provide(sidebarExpandedFolderIdsKey, expandedFolderIds)
provide(sidebarToggleFolderIdKey, toggleFolderId)
provide(sidebarStructuralSidebarTitlesKey, structuralSidebarTitles)
provide(sidebarActiveNoteFolderIdsKey, activeNoteFolderIds)
provide(sidebarUserActiveFolderIdKey, userActiveFolderId)

watch(
  () => props.activeNoteRealm,
  (realm) => {
    ensureFolderExpanded(realm?.note?.noteTopology?.folderId)
  },
  { immediate: true, deep: true }
)

watch(
  () => props.notebookId,
  (notebookId, previousNotebookId) => {
    if (previousNotebookId !== undefined && notebookId !== previousNotebookId) {
      expandedFolderIds.value = new Set()
      userActiveFolderId.value = null
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

const noteContextResolved = computed(
  () => props.activeNoteRealm?.note?.noteTopology != null
)

/** Notebook overview pages may load root notes without an anchor note (e.g. no index note). */
const sidebarTreeShown = computed(
  () =>
    props.activeNoteRealm === undefined ||
    props.activeNoteRealm.note.noteTopology != null
)
</script>
