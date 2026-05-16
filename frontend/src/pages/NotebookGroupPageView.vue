<template>
  <GlobalBar>
    <div class="flex min-w-0 items-center gap-3">
      <button
        type="button"
        class="daisy-btn daisy-btn-sm daisy-btn-ghost shrink-0"
        @click="goToNotebooks"
      >
        Back to notebooks
      </button>
      <h1
        class="m-0 truncate self-center text-2xl font-bold tracking-tight text-base-content"
      >
        {{ group.name }}
      </h1>
    </div>
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
    </template>
  </GlobalBar>
  <main
    class="container mx-auto px-4 py-6"
    :class="
      notebooksLayout === 'grid' ? 'max-w-7xl' : 'max-w-3xl'
    "
  >
    <section class="mb-12">
      <NotebookCatalogGroupPanel
        :group="group"
        :layout="notebooksLayout"
        :subscriptions="subscriptions"
        :user="user"
        :header-navigates-to-group="false"
        :member-preview-limit="null"
        compact-members
        @notebook-updated="emit('notebook-updated', $event)"
        @refresh="emit('refresh')"
      />
    </section>
  </main>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { LayoutGrid, List } from "lucide-vue-next"
import type {
  Notebook,
  NotebookCatalogGroupItem,
  SubscriptionForNotebooksListing,
  User,
} from "@generated/doughnut-backend-api"
import NotebookCatalogGroupPanel from "@/components/notebook/NotebookCatalogGroupPanel.vue"
import { useNotebooksLayout } from "@/composables/useNotebooksLayout"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import { useRouter } from "vue-router"

defineProps({
  group: {
    type: Object as PropType<NotebookCatalogGroupItem>,
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

const router = useRouter()
const { notebooksLayout } = useNotebooksLayout()

const goToNotebooks = () => {
  router.push({ name: "notebooks" })
}
</script>
