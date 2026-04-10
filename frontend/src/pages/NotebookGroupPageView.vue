<template>
  <GlobalBar>
    <div class="daisy-flex daisy-min-w-0 daisy-items-center daisy-gap-3">
      <button
        type="button"
        class="daisy-btn daisy-btn-sm daisy-btn-ghost daisy-shrink-0"
        @click="goToNotebooks"
      >
        Back to notebooks
      </button>
      <h1
        class="daisy-m-0 daisy-truncate daisy-self-center daisy-text-2xl daisy-font-bold daisy-tracking-tight daisy-text-base-content"
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
    </template>
  </GlobalBar>
  <main
    class="daisy-container daisy-mx-auto daisy-px-4 daisy-py-6"
    :class="
      notebooksLayout === 'grid' ? 'daisy-max-w-7xl' : 'daisy-max-w-3xl'
    "
  >
    <section class="daisy-mb-12">
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
  Subscription,
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

const router = useRouter()
const { notebooksLayout } = useNotebooksLayout()

const goToNotebooks = () => {
  router.push({ name: "notebooks" })
}
</script>
