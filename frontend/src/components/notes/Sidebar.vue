<template>
  <div class="daisy-ml-[-1rem]">
    <NoteSidebarToolbar
      v-if="!sidebarReadonly"
      :notebook-id="notebookId"
      :note="activeNoteRealm?.note"
      :active-note-topology-resolved="noteContextResolved"
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
import type {
  NoteRealm,
  NoteTopology,
  Note,
  User,
} from "@generated/doughnut-backend-api"
import NoteSidebarToolbar from "./NoteSidebarToolbar.vue"
import SidebarInner from "./SidebarInner.vue"
import {
  noteSlugFolderPrefixes,
  sidebarActiveNoteFolderSlugPrefixesKey,
  sidebarExpandedFolderSlugsKey,
  sidebarToggleFolderSlugKey,
} from "./sidebarFolderExpansion"
import {
  type SidebarNoteDragState,
  sidebarNoteDragStateKey,
} from "./sidebarNoteDragContext"

const props = defineProps({
  /** When set, highlights the active note and expands its ancestors */
  activeNoteRealm: {
    type: Object as PropType<NoteRealm | undefined>,
    required: false,
  },
  /** Notebook id for sidebar chrome; toolbar shows root create until active note topology exists */
  notebookId: { type: Number, required: true },
})

const expandedFolderSlugs = ref<Set<string>>(new Set())

function ensureFolderExpanded(folderSlug: string) {
  if (folderSlug === "") return
  if (expandedFolderSlugs.value.has(folderSlug)) return
  expandedFolderSlugs.value = new Set([
    ...expandedFolderSlugs.value,
    folderSlug,
  ])
}

function toggleFolderSlug(folderSlug: string) {
  const next = new Set(expandedFolderSlugs.value)
  if (next.has(folderSlug)) {
    next.delete(folderSlug)
  } else {
    next.add(folderSlug)
  }
  expandedFolderSlugs.value = next
}

function expandFolderSlugsForTopologySlug(slug: string | undefined) {
  if (!slug) return
  for (const prefix of noteSlugFolderPrefixes(slug)) {
    ensureFolderExpanded(prefix)
  }
  ensureFolderExpanded(slug)
}

function walkAncestorTopologies(
  start: NoteTopology | undefined,
  fn: (t: NoteTopology) => void
) {
  let cursor = start
  while (cursor) {
    fn(cursor)
    cursor = cursor.parentOrSubjectNoteTopology
  }
}

const activeNoteFolderSlugPrefixes = computed(() => {
  const prefixes = new Set<string>()
  const note = props.activeNoteRealm?.note
  const slug = note?.noteTopology?.slug
  if (slug) {
    for (const p of noteSlugFolderPrefixes(slug)) {
      prefixes.add(p)
    }
    prefixes.add(slug)
  }
  walkAncestorTopologies(
    note?.noteTopology.parentOrSubjectNoteTopology,
    (topology) => {
      const s = topology.slug
      if (!s) return
      for (const p of noteSlugFolderPrefixes(s)) {
        prefixes.add(p)
      }
      prefixes.add(s)
    }
  )
  return prefixes
})

const sidebarNoteDrag: SidebarNoteDragState = {
  draggedNote: ref<Note | null>(null),
  isDraggedOver: ref<number | null>(null),
  dropMode: ref<"after" | "asFirstChild">("after"),
  dropIndicatorStyle: ref({}),
}

provide(sidebarExpandedFolderSlugsKey, expandedFolderSlugs)
provide(sidebarToggleFolderSlugKey, toggleFolderSlug)
provide(sidebarActiveNoteFolderSlugPrefixesKey, activeNoteFolderSlugPrefixes)
provide(sidebarNoteDragStateKey, sidebarNoteDrag)

watch(
  () => props.activeNoteRealm?.note,
  (note) => {
    if (!note?.noteTopology) return
    expandFolderSlugsForTopologySlug(note.noteTopology.slug)
    walkAncestorTopologies(
      note.noteTopology.parentOrSubjectNoteTopology,
      (topology) => {
        expandFolderSlugsForTopologySlug(topology.slug)
      }
    )
  },
  { immediate: true, deep: true }
)

watch(
  () => props.notebookId,
  (notebookId, previousNotebookId) => {
    if (previousNotebookId !== undefined && notebookId !== previousNotebookId) {
      expandedFolderSlugs.value = new Set()
      sidebarNoteDrag.draggedNote.value = null
      sidebarNoteDrag.isDraggedOver.value = null
      sidebarNoteDrag.dropMode.value = "after"
      sidebarNoteDrag.dropIndicatorStyle.value = {}
    }
  }
)

const currentUser = inject<Ref<User | undefined>>("currentUser")
const sidebarReadonly = computed(
  () =>
    !currentUser?.value ||
    (props.activeNoteRealm != null && props.activeNoteRealm.fromBazaar === true)
)

const noteContextResolved = computed(
  () => props.activeNoteRealm?.note?.noteTopology != null
)

/** Notebook overview pages may load root notes without an anchor note (e.g. no `index` slug). */
const sidebarTreeShown = computed(
  () =>
    props.activeNoteRealm === undefined ||
    props.activeNoteRealm.note.noteTopology != null
)
</script>
