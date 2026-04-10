<template>
  <ContainerPage
    v-bind="{ contentLoaded: circle !== undefined, title: `Circle: ${circle?.name}` }"
  >
    <div v-if="circle">
      <p>
        <NotebookNewButton :circle="circle">
          Add New Notebook In This Circle
        </NotebookNewButton>
      </p>

      <div
        v-if="user && catalogItems.length > 0"
        class="daisy-mb-4"
      >
        <div class="daisy-flex daisy-flex-wrap daisy-items-center daisy-justify-between daisy-gap-2">
          <div class="daisy-join">
            <button
              type="button"
              class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-join-item"
              :class="{ 'daisy-btn-active': notebooksLayout === 'list' }"
              title="List view"
              aria-label="List view"
              data-cy="circle-catalog-list-view"
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
              data-cy="circle-catalog-grid-view"
              :aria-pressed="notebooksLayout === 'grid'"
              @click="notebooksLayout = 'grid'"
            >
              <LayoutGrid class="h-5 w-5" />
            </button>
          </div>
          <div class="daisy-form-control daisy-w-auto">
            <label class="daisy-label daisy-sr-only" for="circle-notebook-catalog-sort">
              Sort notebooks
            </label>
            <select
              id="circle-notebook-catalog-sort"
              v-model="notebooksSortOrder"
              class="daisy-select daisy-select-bordered daisy-select-sm daisy-w-max daisy-max-w-[12rem]"
              title="Sort notebooks"
            >
              <option value="created">By created time</option>
              <option value="alphabetical">Alphabetical</option>
            </select>
          </div>
        </div>
        <label
          class="daisy-label daisy-px-0 daisy-pt-3 daisy-pb-1"
          for="circle-notebook-filter-input"
        >
          <span class="daisy-label-text daisy-text-sm daisy-text-base-content/70">
            Filter notebooks
          </span>
        </label>
        <div class="daisy-flex daisy-items-center daisy-gap-2">
          <div class="daisy-input daisy-input-bordered daisy-input-sm daisy-flex daisy-items-center daisy-gap-2 daisy-flex-1">
            <Search class="h-4 w-4 daisy-text-base-content/50" />
            <input
              id="circle-notebook-filter-input"
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

      <main
        :class="
          notebooksLayout === 'grid' ? 'daisy-max-w-7xl' : 'daisy-max-w-3xl'
        "
      >
        <NotebookCatalogSection
          v-if="user && filteredCatalogItems.length > 0"
          :catalog-items="filteredCatalogItems"
          :subscriptions="circle.notebooks.subscriptions ?? []"
          :layout="notebooksLayout"
          :user="user"
          :catalog-filter-active="filterText.trim().length > 0"
          @notebook-updated="handleNotebookUpdated"
          @refresh="fetchData"
        />
        <div
          v-else-if="user && catalogItems.length > 0"
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
          v-else-if="user && catalogItems.length === 0"
          class="daisy-rounded-box daisy-border daisy-border-dashed daisy-border-base-300 daisy-bg-base-200/30 daisy-px-6 daisy-py-10 daisy-text-center"
        >
          <p class="daisy-m-0 daisy-text-base daisy-text-base-content/70">
            This circle has no notebooks yet.
          </p>
        </div>
      </main>

      <nav class="daisy-flex daisy-justify-end">
        <div
          class="daisy-flex-none circle-member"
          v-for="member in circle.members"
          :key="member.name"
        >
          <span :title="member.name"> <UserIcon class="w-6 h-6" /> </span>
        </div>
      </nav>

      <h2>Invite People To Your Circle</h2>
      Please share this invitation code so that they can join your circle:

      <div class="daisy-hero daisy-bg-base-200">
        <input id="invitation-code" :value="invitationUrl" readonly class="daisy-input daisy-input-bordered daisy-w-full" />
      </div>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import NotebookCatalogSection from "@/components/notebook/NotebookCatalogSection.vue"
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue"
import { catalogMoveToGroupContextKey } from "@/components/notebook/catalogMoveToGroupContext"
import { narrowGroupNotebooksForCatalogFilter } from "@/components/notebook/narrowGroupNotebooksForCatalogFilter"
import {
  patchNotebookInCatalogItems,
  type NotebookCatalogEntry,
} from "@/components/notebook/patchNotebookInCatalogItems"
import { sortNotebookCatalogAlphabetically } from "@/components/notebook/sortNotebookCatalogAlphabetically"
import { useNotebooksLayout } from "@/composables/useNotebooksLayout"
import {} from "@/managedApi/clientSetup"
import { LayoutGrid, List, Search, User as UserIcon, X } from "lucide-vue-next"
import type {
  CircleForUserView,
  Notebook,
  NotebookCatalogGroupItem,
  User,
} from "@generated/doughnut-backend-api"
import { CircleController } from "@generated/doughnut-backend-api/sdk.gen"
import type { Ref } from "vue"
import { computed, inject, onMounted, provide, ref, watch } from "vue"
import { useRouter } from "vue-router"
import ContainerPage from "./commons/ContainerPage.vue"

const CIRCLE_NOTEBOOKS_SORT_STORAGE_KEY = "doughnut.circlePage.sortOrder"

const router = useRouter()

const { circleId } = defineProps({
  circleId: { type: Number, required: true },
})

const user = inject<Ref<User | undefined>>("currentUser")
const circle = ref<CircleForUserView | undefined>(undefined)

const { notebooksLayout } = useNotebooksLayout()
const notebooksSortOrder = ref<"created" | "alphabetical">("created")
const filterText = ref("")

const catalogItems = computed((): NotebookCatalogEntry[] => {
  if (!circle.value?.notebooks.catalogItems) {
    return []
  }
  return circle.value.notebooks.catalogItems as NotebookCatalogEntry[]
})

const catalogMoveToGroupContext = computed(() => {
  if (!circle.value) {
    return undefined
  }
  const items = catalogItems.value
  const existingGroups = items
    .filter((i): i is NotebookCatalogGroupItem => i.type === "notebookGroup")
    .map((g) => ({ id: g.id, name: g.name }))
  return { circleId: circle.value.id, existingGroups }
})

provide(catalogMoveToGroupContextKey, catalogMoveToGroupContext)

const filteredOnlyCatalogItems = computed(() => {
  const q = filterText.value.trim().toLowerCase()
  const items = catalogItems.value
  if (!q) {
    return items
  }
  const rows = items.filter((item) => {
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

const filteredCatalogItems = computed(() => {
  const items = filteredOnlyCatalogItems.value
  if (notebooksSortOrder.value === "created") {
    return items
  }
  return sortNotebookCatalogAlphabetically(items)
})

const clearFilter = () => {
  filterText.value = ""
}

const fetchData = async () => {
  const { data: circleData, error } = await CircleController.showCircle({
    path: { circle: circleId },
  })
  if (!error) {
    circle.value = circleData!
  }
}

const handleNotebookUpdated = (updatedNotebook: Notebook) => {
  if (!circle.value) {
    return
  }
  const list = circle.value.notebooks.notebooks
  const index = list.findIndex((n) => n.id === updatedNotebook.id)
  if (index !== -1) {
    list[index] = updatedNotebook
  }
  circle.value.notebooks.catalogItems = patchNotebookInCatalogItems(
    catalogItems.value,
    updatedNotebook
  ) as CircleForUserView["notebooks"]["catalogItems"]
}

const invitationUrl = computed(
  () =>
    `${window.location.origin}${
      router.resolve({
        name: "circleJoin",
        params: { invitationCode: circle.value?.invitationCode },
      }).href
    }`
)

onMounted(() => {
  const storedSort = localStorage.getItem(CIRCLE_NOTEBOOKS_SORT_STORAGE_KEY)
  if (storedSort === "created" || storedSort === "alphabetical") {
    notebooksSortOrder.value = storedSort
  }
  fetchData()
})

watch(notebooksSortOrder, (value) => {
  localStorage.setItem(CIRCLE_NOTEBOOKS_SORT_STORAGE_KEY, value)
})
</script>

<style lang="sass" scoped>
#invitation-code
  width: 100%
</style>
