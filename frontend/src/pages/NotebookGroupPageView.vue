<template>
  <GlobalBar>
    <div
      v-if="group"
      class="flex min-w-0 items-center gap-3"
    >
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
    <template
      v-if="group"
      #right
    >
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
  <ContentLoader v-if="group === undefined" />
  <main
    v-else-if="group === null"
    class="container mx-auto max-w-3xl px-4 py-6"
  >
    <div
      class="rounded-box border border-dashed border-base-300 bg-base-200/30 px-6 py-10 text-center"
    >
      <p class="m-0 text-base text-base-content/70">
        This notebook group could not be found.
      </p>
      <router-link
        :to="{ name: 'notebooks' }"
        class="daisy-btn daisy-btn-link daisy-btn-sm mt-3 inline-block"
      >
        Back to Notebooks
      </router-link>
    </div>
  </main>
  <main
    v-else
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
import { LayoutGrid, List } from "@lucide/vue"
import type {
  Notebook,
  NotebookCatalogGroupItem,
  SubscriptionForNotebooksListing,
  User,
} from "@generated/donut-backend-api"
import NotebookCatalogGroupPanel from "@/components/notebook/NotebookCatalogGroupPanel.vue"
import { useNotebooksLayout } from "@/composables/useNotebooksLayout"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import { useRouter } from "vue-router"

defineProps({
  // Undefined while the catalog loads; null when the catalog has no such group.
  group: {
    type: Object as PropType<NotebookCatalogGroupItem | null>,
    required: false,
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
