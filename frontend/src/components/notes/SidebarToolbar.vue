<template>
  <nav
    :class="[noteChromeToolbarNavClass, 'overflow-visible']"
    data-note-sidebar-toolbar
  >
    <div
      class="flex flex-row items-center overflow-visible min-w-0"
    >
      <div class="daisy-btn-group daisy-btn-group-sm overflow-visible">
        <NoteCreationNewButton
          v-if="sidebarOpened"
          :notebook-id="notebookId"
          :active-note-realm="activeNoteRealm"
          :active-folder-realm="activeFolderRealm"
          :breadcrumb-folders="breadcrumbFolders"
        />
        <FolderNewButton
          :notebook-id="notebookId"
          :ancestor-folders="breadcrumbFolders"
          :context-folder="parentFolderForCreation"
          button-title="New folder"
          aria-label="New folder"
        >
          <FolderPlus class="w-6 h-6" />
        </FolderNewButton>
      </div>
      <AutoCollapseDropdown
        v-slot="{ closeDropdown }"
        data-note-sidebar-sort
        class="daisy-dropdown daisy-dropdown-start daisy-dropdown-bottom shrink-0"
      >
        <summary
          class="daisy-btn daisy-btn-ghost daisy-btn-sm rounded-none list-none cursor-pointer"
          aria-label="Sort sidebar"
          title="Sort sidebar"
        >
          <component :is="triggerIcon" class="w-6 h-6" aria-hidden="true" />
        </summary>
        <SidebarPeerSortDropdownMenu
          @select="(spec) => selectSort(spec, closeDropdown)"
        />
      </AutoCollapseDropdown>
    </div>
  </nav>
</template>

<script setup lang="ts">
import type {
  Folder,
  FolderRealm,
  NoteRealm,
} from "@generated/doughnut-backend-api"
import { SIDEBAR_PEER_SORT_MENU_ROWS } from "@/composables/sidebarPeerSortMenuRows"
import SidebarPeerSortDropdownMenu from "./SidebarPeerSortDropdownMenu.vue"
import {
  useNoteSidebarPeerSort,
  type SidebarPeerSortSpec,
} from "@/composables/useNoteSidebarPeerSort"
import { ArrowDownAZ, FolderPlus } from "@lucide/vue"
import { computed } from "vue"
import AutoCollapseDropdown from "@/components/commons/AutoCollapseDropdown.vue"
import FolderNewButton from "./core/FolderNewButton.vue"
import NoteCreationNewButton from "./NoteCreationNewButton.vue"
import { noteChromeToolbarNavClass } from "./noteChromeToolbarNavClass"
import { useNoteCreationToolbarContext } from "@/composables/useNoteCreationToolbarContext"
import { useNotebookSidebarOpened } from "@/composables/notebookSidebarOpened"

const props = defineProps<{
  notebookId: number
  activeNoteRealm?: NoteRealm
  activeFolderRealm?: FolderRealm
  breadcrumbFolders: Folder[]
}>()

const sidebarOpened = useNotebookSidebarOpened()

const { parentFolderForCreation } = useNoteCreationToolbarContext(() => ({
  activeNoteRealm: props.activeNoteRealm,
  activeFolderRealm: props.activeFolderRealm,
}))

const { sortPeerSpec, setSortPeerSpec } = useNoteSidebarPeerSort()

const triggerIcon = computed(() => {
  const match = SIDEBAR_PEER_SORT_MENU_ROWS.find(
    (row) =>
      row.spec.field === sortPeerSpec.value.field &&
      row.spec.direction === sortPeerSpec.value.direction
  )
  return match?.Icon ?? ArrowDownAZ
})

function selectSort(spec: SidebarPeerSortSpec, closeDropdown: () => void) {
  setSortPeerSpec(spec)
  closeDropdown()
}
</script>
