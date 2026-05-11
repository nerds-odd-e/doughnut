<template>
  <nav
    :class="[noteChromeToolbarNavClass, 'daisy-overflow-visible daisy-relative daisy-z-20']"
    data-note-sidebar-toolbar
  >
    <div
      class="daisy-flex daisy-flex-row daisy-items-center daisy-overflow-visible daisy-min-w-0"
    >
      <div class="daisy-btn-group daisy-btn-group-sm daisy-overflow-visible">
        <NoteNewButton
          :notebook-id="notebookId"
          :initial-folder="parentFolderForCreation ?? undefined"
          :title-search-anchor-note="anchorNote"
          :ancestor-folders="breadcrumbFolders"
          :initial-title="initialTitle"
        >
          <NotebookPen class="daisy-w-6 daisy-h-6" />
        </NoteNewButton>
        <FolderNewButton
          :notebook-id="notebookId"
          :ancestor-folders="breadcrumbFolders"
          :context-folder="parentFolderForCreation"
          button-title="New folder"
          aria-label="New folder"
        >
          <FolderPlus class="daisy-w-6 daisy-h-6" />
        </FolderNewButton>
        <FolderOrganizeButton
          v-if="activeFolderRealm"
          :moving-folder-realm="activeFolderRealm"
        >
          <FolderInput class="daisy-w-6 daisy-h-6" />
        </FolderOrganizeButton>
      </div>
      <details
        ref="sortDropdownRef"
        data-note-sidebar-sort
        class="daisy-dropdown daisy-dropdown-start daisy-dropdown-bottom daisy-relative daisy-z-30 daisy-shrink-0"
      >
        <summary
          class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-rounded-none list-none daisy-cursor-pointer"
          aria-label="Sort sidebar"
          title="Sort sidebar"
        >
          <component :is="triggerIcon" class="daisy-w-6 daisy-h-6" aria-hidden="true" />
        </summary>
        <ul
          tabindex="0"
          class="daisy-dropdown-content daisy-menu daisy-bg-base-100 daisy-rounded-box daisy-min-w-[16rem] daisy-w-[17.5rem] daisy-max-w-[17.5rem] daisy-p-2 daisy-shadow daisy-z-[1000]"
        >
          <li
            v-for="row in SIDEBAR_PEER_SORT_MENU_ROWS"
            :key="`${row.spec.field}-${row.spec.direction}`"
            class="daisy-menu-item daisy-p-0"
          >
            <button
              type="button"
              class="daisy-btn daisy-btn-ghost daisy-h-auto daisy-min-h-0 daisy-w-full daisy-justify-start daisy-gap-2 daisy-py-2 daisy-font-normal daisy-whitespace-normal daisy-items-start daisy-text-left"
              :title="row.label"
              @click="selectSort(row.spec)"
            >
              <component
                :is="row.Icon"
                :size="14"
                class="daisy-mt-0.5 daisy-shrink-0"
                aria-hidden="true"
              />
              <span class="daisy-min-w-0 daisy-text-left daisy-leading-snug">{{
                row.label
              }}</span>
            </button>
          </li>
        </ul>
      </details>
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
import {
  ArrowDownAZ,
  FolderInput,
  FolderPlus,
  NotebookPen,
} from "lucide-vue-next"
import { computed, ref } from "vue"
import FolderNewButton from "./core/FolderNewButton.vue"
import FolderOrganizeButton from "./core/FolderOrganizeButton.vue"
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

const sortDropdownRef = ref<HTMLDetailsElement | null>(null)

const triggerIcon = computed(() => {
  const match = SIDEBAR_PEER_SORT_MENU_ROWS.find(
    (row) =>
      row.spec.field === sortPeerSpec.value.field &&
      row.spec.direction === sortPeerSpec.value.direction
  )
  return match?.Icon ?? ArrowDownAZ
})

function selectSort(spec: SidebarPeerSortSpec) {
  setSortPeerSpec(spec)
  if (sortDropdownRef.value) {
    sortDropdownRef.value.open = false
  }
}
</script>
