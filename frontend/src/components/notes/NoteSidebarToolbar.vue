<template>
  <nav
    :class="[noteChromeToolbarNavClass, 'daisy-overflow-visible daisy-relative daisy-z-20']"
    data-note-sidebar-toolbar
  >
    <div
      class="daisy-flex daisy-flex-row daisy-items-center daisy-overflow-visible daisy-min-w-0"
    >
      <div class="daisy-btn-group daisy-btn-group-sm daisy-overflow-visible">
        <NotebookRootNoteNewButton
          :notebook-id="notebookId"
          :target-folder-id="resolvedCreateParentFolderId ?? undefined"
          :parent-location-description="createParentLocationDescription"
          :title-search-anchor-note="note"
          button-title="New note"
          aria-label="New note"
        >
          <NotebookPen class="w-5 h-5" />
        </NotebookRootNoteNewButton>
        <FolderNewButton
          :notebook-id="notebookId"
          :under-folder-id="folderUnderFolderId"
          :under-note-id="folderContextNoteId"
          :parent-location-description="createParentLocationDescription"
          button-title="New folder"
          aria-label="New folder"
        >
          <FolderPlus class="w-5 h-5" />
        </FolderNewButton>
        <FolderOrganizeButton
          v-if="userActiveFolder != null"
          :notebook-id="notebookId"
          :moving-folder-id="userActiveFolder.id"
          :moving-folder-name="userActiveFolder.name"
        >
          <FolderInput class="w-5 h-5" />
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
          <component :is="triggerIcon" class="w-5 h-5" aria-hidden="true" />
        </summary>
        <ul
          tabindex="0"
          class="daisy-dropdown-content daisy-menu daisy-bg-base-100 daisy-rounded-box daisy-min-w-[16rem] daisy-w-[17.5rem] daisy-max-w-[17.5rem] daisy-p-2 daisy-shadow daisy-z-[1000]"
        >
          <li
            v-for="row in sortMenuRows"
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
import type { Note } from "@generated/doughnut-backend-api"
import {
  useNoteSidebarPeerSort,
  type SidebarPeerSortSpec,
} from "@/composables/useNoteSidebarPeerSort"
import {
  ArrowDownAZ,
  ArrowUpAZ,
  CalendarArrowDown,
  CalendarArrowUp,
  ClockArrowDown,
  ClockArrowUp,
  FolderInput,
  FolderPlus,
  NotebookPen,
} from "lucide-vue-next"
import { computed, ref } from "vue"
import FolderNewButton from "./core/FolderNewButton.vue"
import FolderOrganizeButton from "./core/FolderOrganizeButton.vue"
import NotebookRootNoteNewButton from "./core/NotebookRootNoteNewButton.vue"
import { noteChromeToolbarNavClass } from "./noteChromeToolbarNavClass"
import type { SidebarUserActiveFolder } from "./useNoteSidebarTree"

const props = defineProps<{
  notebookId: number
  note?: Note
  activeNoteTopologyResolved: boolean
  /** Parent folder for new note / new folder (active sidebar folder, else active note's folder). */
  resolvedCreateParentFolderId: number | null
  createParentLocationDescription: string
  userActiveFolder: SidebarUserActiveFolder | null
}>()

const { sortPeerSpec, setSortPeerSpec } = useNoteSidebarPeerSort()

const sortDropdownRef = ref<HTMLDetailsElement | null>(null)

const sortMenuRows: {
  spec: SidebarPeerSortSpec
  label: string
  Icon: typeof ArrowDownAZ
}[] = [
  {
    spec: { field: "title", direction: "asc" },
    label: "Title (A–Z)",
    Icon: ArrowDownAZ,
  },
  {
    spec: { field: "title", direction: "desc" },
    label: "Title (Z–A)",
    Icon: ArrowUpAZ,
  },
  {
    spec: { field: "created", direction: "asc" },
    label: "Created (oldest first)",
    Icon: CalendarArrowDown,
  },
  {
    spec: { field: "created", direction: "desc" },
    label: "Created (newest first)",
    Icon: CalendarArrowUp,
  },
  {
    spec: { field: "updated", direction: "asc" },
    label: "Updated (oldest first)",
    Icon: ClockArrowDown,
  },
  {
    spec: { field: "updated", direction: "desc" },
    label: "Updated (newest first)",
    Icon: ClockArrowUp,
  },
]

const triggerIcon = computed(() => {
  const match = sortMenuRows.find(
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

const folderUnderFolderId = computed(() =>
  props.resolvedCreateParentFolderId != null
    ? props.resolvedCreateParentFolderId
    : undefined
)

const folderContextNoteId = computed(() =>
  props.resolvedCreateParentFolderId != null
    ? undefined
    : props.note != null && props.activeNoteTopologyResolved
      ? props.note.id
      : undefined
)
</script>
