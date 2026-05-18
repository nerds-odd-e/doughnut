<template>
  <nav
    :class="[noteChromeToolbarNavClass, 'overflow-visible relative z-20']"
    data-note-sidebar-toolbar
  >
    <div
      class="flex flex-row items-center overflow-visible min-w-0"
    >
      <div class="daisy-btn-group daisy-btn-group-sm overflow-visible">
        <NoteNewButton
          :notebook-id="notebookId"
          :initial-folder="parentFolderForCreation ?? undefined"
          :title-search-anchor-note="anchorNote"
          :ancestor-folders="breadcrumbFolders"
          :initial-title="initialTitle"
        >
          <NotebookPen class="w-6 h-6" />
        </NoteNewButton>
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
        class="daisy-dropdown daisy-dropdown-start daisy-dropdown-bottom relative z-30 shrink-0"
      >
        <summary
          class="daisy-btn daisy-btn-ghost daisy-btn-sm rounded-none list-none cursor-pointer"
          aria-label="Sort sidebar"
          title="Sort sidebar"
        >
          <component :is="triggerIcon" class="w-6 h-6" aria-hidden="true" />
        </summary>
        <ul
          tabindex="0"
          class="daisy-dropdown-content daisy-menu bg-base-100 rounded-box min-w-[16rem] w-[17.5rem] max-w-[17.5rem] p-2 shadow z-[1000]"
        >
          <li
            v-for="row in SIDEBAR_PEER_SORT_MENU_ROWS"
            :key="`${row.spec.field}-${row.spec.direction}`"
            class="daisy-menu-item p-0"
          >
            <button
              type="button"
              class="daisy-btn daisy-btn-ghost h-auto min-h-0 w-full justify-start gap-2 py-2 font-normal whitespace-normal items-start text-left"
              :title="row.label"
              @click="selectSort(row.spec, closeDropdown)"
            >
              <component
                :is="row.Icon"
                :size="14"
                class="mt-0.5 shrink-0"
                aria-hidden="true"
              />
              <span class="min-w-0 text-left leading-snug">{{
                row.label
              }}</span>
            </button>
          </li>
        </ul>
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
import {
  useNoteSidebarPeerSort,
  type SidebarPeerSortSpec,
} from "@/composables/useNoteSidebarPeerSort"
import { ArrowDownAZ, FolderPlus, NotebookPen } from "@lucide/vue"
import { computed } from "vue"
import AutoCollapseDropdown from "@/components/commons/AutoCollapseDropdown.vue"
import FolderNewButton from "./core/FolderNewButton.vue"
import NoteNewButton from "./core/NoteNewButton.vue"
import { noteChromeToolbarNavClass } from "./noteChromeToolbarNavClass"
import { realmLeafFolder } from "./useNoteSidebarTree"
import { titlePatternFromNoteMarkdown } from "@/utils/noteContentFrontmatter"
import { renderTitleFromPattern } from "@/utils/titlePatternRender"

const props = defineProps<{
  notebookId: number
  activeNoteRealm?: NoteRealm
  activeFolderRealm?: FolderRealm
  breadcrumbFolders: Folder[]
}>()

const initialTitle = computed(() => {
  const markdown =
    props.activeNoteRealm?.indexNoteContent ??
    props.activeFolderRealm?.indexNoteContent ??
    null
  const pattern = titlePatternFromNoteMarkdown(markdown)
  if (pattern == null || pattern === "") return undefined
  return renderTitleFromPattern(pattern)
})

const parentFolderForCreation = computed((): Folder | null => {
  if (props.activeFolderRealm) return props.activeFolderRealm.folder
  return realmLeafFolder(props.activeNoteRealm) ?? null
})

const anchorNote = computed(() => props.activeNoteRealm?.note)

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
