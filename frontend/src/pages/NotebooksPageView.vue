<template>
  <GlobalBar>
    <h1
      class="daisy-m-0 daisy-self-center daisy-text-2xl daisy-font-bold daisy-tracking-tight daisy-text-base-content"
    >
      My notebooks
    </h1>
    <template #right>
      <div class="daisy-flex daisy-items-center daisy-gap-2">
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
            <List class="daisy-h-6 daisy-w-6" />
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
            <LayoutGrid class="daisy-h-6 daisy-w-6" />
          </button>
        </div>
        <details
          ref="notebooksSortDropdownRef"
          data-testid="notebook-catalog-sort"
          class="daisy-dropdown daisy-dropdown-start daisy-dropdown-bottom daisy-relative daisy-z-30 daisy-shrink-0"
        >
          <summary
            class="daisy-btn daisy-btn-ghost daisy-btn-sm list-none daisy-cursor-pointer"
            aria-label="Sort notebooks"
            title="Sort notebooks"
          >
            <component
              :is="catalogPeerSortTriggerIcon"
              class="daisy-h-6 daisy-w-6"
              aria-hidden="true"
            />
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
                :data-catalog-sort="`${row.spec.field}-${row.spec.direction}`"
                @click="selectCatalogPeerSort(row.spec)"
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
    class="daisy-container daisy-mx-auto daisy-px-4 daisy-py-6"
    :class="
      notebooksLayout === 'grid' ? 'daisy-max-w-7xl' : 'daisy-max-w-3xl'
    "
  >
    <section class="daisy-mb-12">
      <div
        v-if="catalogItems.length > 0"
        class="daisy-mb-4"
      >
        <label
          class="daisy-label daisy-px-0 daisy-pb-1"
          for="notebook-filter-input"
        >
          <span class="daisy-label-text daisy-text-sm daisy-text-base-content/70">
            Filter notebooks
          </span>
        </label>
        <div class="daisy-flex daisy-items-center daisy-gap-2">
          <div class="daisy-input daisy-input-bordered daisy-input-sm daisy-flex daisy-items-center daisy-gap-2 daisy-flex-1">
            <Search class="daisy-h-4 daisy-w-4 daisy-text-base-content/50" />
            <input
              id="notebook-filter-input"
              ref="filterInputRef"
              v-model="filterText"
              type="text"
              class="daisy-grow"
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
            <X class="daisy-h-4 daisy-w-4" />
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
        class="daisy-rounded-box daisy-border daisy-border-dashed daisy-border-base-300 daisy-bg-base-200/30 daisy-px-6 daisy-py-10 daisy-text-center"
      >
        <p class="daisy-m-0 daisy-text-base daisy-text-base-content/70">
          No notebooks match
        </p>
        <button
          type="button"
          class="daisy-btn daisy-btn-link daisy-btn-sm daisy-mt-2"
          @click="clearFilter"
        >
          Clear filter
        </button>
      </div>
      <div
        v-else
        class="daisy-rounded-box daisy-border daisy-border-dashed daisy-border-base-300 daisy-bg-base-200/30 daisy-px-6 daisy-py-10 daisy-text-center"
      >
        <p class="daisy-m-0 daisy-text-base daisy-text-base-content/70">
          You do not have any notebooks yet.
        </p>
        <p class="daisy-mt-2 daisy-mb-0 daisy-text-sm daisy-text-base-content/50">
          Use <span class="daisy-font-medium daisy-text-base-content/80">Add New Notebook</span> above to create your first one.
        </p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, onMounted, ref } from "vue"
import { ArrowDownAZ, LayoutGrid, List, Search, X } from "lucide-vue-next"
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

const notebooksSortDropdownRef = ref<HTMLDetailsElement | null>(null)

function selectCatalogPeerSort(spec: SidebarPeerSortSpec) {
  setSortPeerSpec(spec)
  if (notebooksSortDropdownRef.value) {
    notebooksSortDropdownRef.value.open = false
  }
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
