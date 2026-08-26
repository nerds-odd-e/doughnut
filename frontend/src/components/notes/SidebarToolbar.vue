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
        <PeerSortDropdownMenu
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
} from "@generated/donut-backend-api"
import { peerSortTriggerIcon } from "@/composables/peerSortMenuRows"
import PeerSortDropdownMenu from "@/components/commons/PeerSortDropdownMenu.vue"
import { usePeerSort, type PeerSortSpec } from "@/composables/usePeerSort"
import { FolderPlus } from "@lucide/vue"
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

const { peerSortSpec, setPeerSortSpec } = usePeerSort()

const triggerIcon = computed(() => peerSortTriggerIcon(peerSortSpec.value))

function selectSort(spec: PeerSortSpec, closeDropdown: () => void) {
  setPeerSortSpec(spec)
  closeDropdown()
}
</script>
