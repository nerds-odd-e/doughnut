<template>
  <GlobalBar>
    <h1
      class="m-0 self-center text-2xl font-bold tracking-tight text-base-content"
    >
      My notebooks
    </h1>
    <template #right>
      <div class="flex items-center gap-2">
        <div class="daisy-join">
          <button
            type="button"
            class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-join-item"
            :class="{ 'daisy-btn-active': notebooksLayout === 'list' }"
            title="List view"
            aria-label="List view"
            :aria-pressed="notebooksLayout === 'list'"
            @click="notebooksLayout = 'list'"
          >
            <List class="h-6 w-6" />
          </button>
          <button
            type="button"
            class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-join-item"
            :class="{ 'daisy-btn-active': notebooksLayout === 'grid' }"
            title="Grid view"
            aria-label="Grid view"
            :aria-pressed="notebooksLayout === 'grid'"
            @click="notebooksLayout = 'grid'"
          >
            <LayoutGrid class="h-6 w-6" />
          </button>
        </div>
        <AutoCollapseDropdown
          v-slot="{ closeDropdown }"
          data-testid="notebook-catalog-sort"
          class="daisy-dropdown daisy-dropdown-start daisy-dropdown-bottom shrink-0"
        >
          <summary
            class="daisy-btn daisy-btn-ghost daisy-btn-sm list-none cursor-pointer"
            aria-label="Sort notebooks"
            title="Sort notebooks"
          >
            <component
              :is="catalogPeerSortTriggerIcon"
              class="h-6 w-6"
              aria-hidden="true"
            />
          </summary>
          <SidebarPeerSortDropdownMenu
            catalog-sort-buttons
            @select="(spec) => selectCatalogPeerSort(spec, closeDropdown)"
          />
        </AutoCollapseDropdown>
        <NotebookNewButton
          v-if="user"
          btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-join-item"
        >
          Add New Notebook
        </NotebookNewButton>
      </div>
    </template>
  </GlobalBar>
  <main
    class="container mx-auto px-4 py-6"
    :class="
      notebooksLayout === 'grid' ? 'max-w-7xl' : 'max-w-3xl'
    "
  >
    <section class="mb-12">
      <div
        v-if="catalogItems.length > 0"
        class="mb-4"
      >
        <label
          class="daisy-label px-0 pb-1"
          for="notebook-filter-input"
        >
          <span class="daisy-label-text text-sm text-base-content/70">
            Filter notebooks
          </span>
        </label>
        <div class="flex items-center gap-2">
          <div class="daisy-input daisy-input-sm flex items-center gap-2 flex-1">
            <Search class="h-4 w-4 text-base-content/50" />
            <input
              id="notebook-filter-input"
              ref="filterInputRef"
              v-model="filterText"
              type="text"
              class="grow"
              placeholder="Search by notebook or group name"
            />
          </div>
          <button
            v-if="filterText.trim().length > 0"
            type="button"
            class="daisy-btn daisy-btn-ghost daisy-btn-sm"
            aria-label="Clear filter"
            @click="clearFilter"
          >
            <X class="h-4 w-4" />
          </button>
        </div>
      </div>
      <NotebookCatalogSection
        v-if="filteredCatalogItems.length > 0"
        :catalog-items="filteredCatalogItems"
        :subscriptions="subscriptions"
        :layout="notebooksLayout"
        :user="user"
        :catalog-filter-active="filterText.trim().length > 0"
        @notebook-updated="handleNotebookUpdated"
        @refresh="emit('refresh')"
      />
      <div
        v-else-if="catalogItems.length > 0"
        class="rounded-box border border-dashed border-base-300 bg-base-200/30 px-6 py-10 text-center"
      >
        <p class="m-0 text-base text-base-content/70">
          No notebooks match
        </p>
        <button
          type="button"
          class="daisy-btn daisy-btn-link daisy-btn-sm mt-2"
          @click="clearFilter"
        >
          Clear filter
        </button>
      </div>
      <div
        v-else
        class="rounded-box border border-dashed border-base-300 bg-base-200/30 px-6 py-10 text-center"
      >
        <p class="m-0 text-base text-base-content/70">
          You do not have any notebooks yet.
        </p>
        <p class="mt-2 mb-0 text-sm text-base-content/50">
          Use <span class="font-medium text-base-content/80">Add New Notebook</span> above to create your first one.
        </p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, onMounted, ref } from "vue"
import { ArrowDownAZ, LayoutGrid, List, Search, X } from "@lucide/vue"
import type {
  Notebook,
  SubscriptionForNotebooksListing,
  User,
} from "@generated/doughnut-backend-api"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"
import { sortNotebookCatalogByPeerSpec } from "@/components/notebook/sortNotebookCatalogByPeerSpec"
import { SIDEBAR_PEER_SORT_MENU_ROWS } from "@/composables/sidebarPeerSortMenuRows"
import { useNotebooksLayout } from "@/composables/useNotebooksLayout"
import {
  useNoteSidebarPeerSort,
  type SidebarPeerSortSpec,
} from "@/composables/useNoteSidebarPeerSort"
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue"
import NotebookCatalogSection from "@/components/notebook/NotebookCatalogSection.vue"
import { narrowGroupNotebooksForCatalogFilter } from "@/components/notebook/narrowGroupNotebooksForCatalogFilter"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import AutoCollapseDropdown from "@/components/commons/AutoCollapseDropdown.vue"
import SidebarPeerSortDropdownMenu from "@/components/notes/SidebarPeerSortDropdownMenu.vue"

const props = defineProps({
  catalogItems: {
    type: Array as PropType<NotebookCatalogEntry[]>,
    required: true,
  },
  subscriptions: {
    type: Array as PropType<SubscriptionForNotebooksListing[]>,
    required: true,
  },
  user: {
    type: Object as PropType<User>,
    required: true,
  },
})

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
  (e: "refresh"): void
}>()

const handleNotebookUpdated = (updatedNotebook: Notebook) => {
  emit("notebook-updated", updatedNotebook)
}

const { notebooksLayout } = useNotebooksLayout()
const { sortPeerSpec, setSortPeerSpec } = useNoteSidebarPeerSort()

const catalogPeerSortTriggerIcon = computed(() => {
  const match = SIDEBAR_PEER_SORT_MENU_ROWS.find(
    (row) =>
      row.spec.field === sortPeerSpec.value.field &&
      row.spec.direction === sortPeerSpec.value.direction
  )
  return match?.Icon ?? ArrowDownAZ
})

function selectCatalogPeerSort(
  spec: SidebarPeerSortSpec,
  closeDropdown: () => void
) {
  setSortPeerSpec(spec)
  closeDropdown()
}
const filterText = ref("")
const filterInputRef = ref<HTMLInputElement | null>(null)

const filteredOnlyCatalogItems = computed(() => {
  const q = filterText.value.trim().toLowerCase()
  if (!q) {
    return props.catalogItems
  }
  const rows = props.catalogItems.filter((item) => {
    if (item.type === "notebook" || item.type === "subscribedNotebook") {
      return (item.notebook.name ?? "").toLowerCase().includes(q)
    }
    if (item.name.toLowerCase().includes(q)) {
      return true
    }
    return item.notebooks.some((nb) =>
      (nb.notebook.name ?? "").toLowerCase().includes(q)
    )
  })
  return rows.map((item) => {
    if (item.type !== "notebookGroup") {
      return item
    }
    return {
      ...item,
      notebooks: narrowGroupNotebooksForCatalogFilter(item, q),
    }
  })
})

const filteredCatalogItems = computed(() =>
  sortNotebookCatalogByPeerSpec(
    filteredOnlyCatalogItems.value,
    sortPeerSpec.value
  )
)

const clearFilter = () => {
  filterText.value = ""
}

onMounted(() => {
  if (props.catalogItems.length > 0) {
    filterInputRef.value?.focus()
  }
})
</script>
