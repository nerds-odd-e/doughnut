<template>
  <GlobalBar>
    <template #right>
      <button
        type="button"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-join-item"
        :class="{ 'daisy-btn-active': notebooksLayout === 'list' }"
        title="List view"
        aria-label="List view"
        :aria-pressed="notebooksLayout === 'list'"
        @click="notebooksLayout = 'list'"
      >
        <List class="h-5 w-5" />
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
        <LayoutGrid class="h-5 w-5" />
      </button>
      <NotebookNewButton
        v-if="user"
        btn-class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-join-item"
      >
        Add New Notebook
      </NotebookNewButton>
    </template>
  </GlobalBar>
  <main
    class="daisy-container daisy-mx-auto daisy-px-4 daisy-py-6"
    :class="
      notebooksLayout === 'grid' ? 'daisy-max-w-7xl' : 'daisy-max-w-3xl'
    "
  >
    <section class="daisy-mb-12">
      <div class="daisy-mb-5 daisy-flex daisy-flex-col daisy-gap-3 sm:daisy-flex-row sm:daisy-items-end sm:daisy-justify-between">
        <div>
          <h1 class="daisy-text-2xl daisy-font-bold daisy-tracking-tight daisy-text-base-content">
            My notebooks
          </h1>
          <p class="daisy-mt-1 daisy-text-sm daisy-text-base-content/60">
            Open a notebook to work with notes, or use the toolbar to create a new one.
            Notebooks you subscribe to in the Bazaar appear in this list together with your own.
          </p>
        </div>
        <div
          v-if="user"
          class="daisy-flex daisy-flex-wrap daisy-items-center daisy-gap-2 sm:daisy-justify-end"
        >
          <PopButton
            title="New notebook group"
            aria-label="New notebook group"
            btn-class="daisy-btn daisy-btn-outline daisy-btn-sm"
          >
            <template #button_face>
              <FolderPlus class="h-4 w-4 sm:daisy-mr-1" />
              New notebook group
            </template>
            <template #default="{ closer }">
              <NotebookGroupNewForm :close="closer" @created="emit('refresh')" />
            </template>
          </PopButton>
        </div>
      </div>
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
            <Search class="h-4 w-4 daisy-text-base-content/50" />
            <input
              id="notebook-filter-input"
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
import { computed, onMounted, ref, watch } from "vue"
import { FolderPlus, LayoutGrid, List, Search, X } from "lucide-vue-next"
import type {
  Notebook,
  Subscription,
  User,
} from "@generated/doughnut-backend-api"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import NotebookGroupNewForm from "@/components/notebook/NotebookGroupNewForm.vue"
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue"
import NotebookCatalogSection from "@/components/notebook/NotebookCatalogSection.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"

const props = defineProps({
  catalogItems: {
    type: Array as PropType<NotebookCatalogEntry[]>,
    required: true,
  },
  subscriptions: {
    type: Array as PropType<Subscription[]>,
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

const NOTEBOOKS_LAYOUT_STORAGE_KEY = "doughnut.notebooksPage.layout"

const notebooksLayout = ref<"list" | "grid">("list")
const filterText = ref("")

const filteredCatalogItems = computed(() => {
  const q = filterText.value.trim().toLowerCase()
  if (!q) {
    return props.catalogItems
  }
  return props.catalogItems.filter((item) => {
    if (item.type === "notebook" || item.type === "subscribedNotebook") {
      return (item.notebook.title ?? "").toLowerCase().includes(q)
    }
    if (item.name.toLowerCase().includes(q)) {
      return true
    }
    return item.notebooks.some((nb) =>
      (nb.title ?? "").toLowerCase().includes(q)
    )
  })
})

const clearFilter = () => {
  filterText.value = ""
}

onMounted(() => {
  const stored = localStorage.getItem(NOTEBOOKS_LAYOUT_STORAGE_KEY)
  if (stored === "list" || stored === "grid") {
    notebooksLayout.value = stored
  }
})

watch(notebooksLayout, (value) => {
  localStorage.setItem(NOTEBOOKS_LAYOUT_STORAGE_KEY, value)
})
</script>
