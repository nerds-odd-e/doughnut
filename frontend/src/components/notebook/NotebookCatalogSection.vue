<template>
  <div
    v-if="layout === 'list'"
    class="notebook-catalog-section notebook-catalog-section--list daisy-flex daisy-flex-col daisy-gap-2"
  >
    <template v-for="item in catalogItems" :key="catalogItemKey(item)">
      <NotebookListRow
        v-if="item.type === 'notebook'"
        :notebook="item.notebook"
      >
        <NotebookButtons
          v-bind="{ notebook: item.notebook, user }"
          @notebook-updated="$emit('notebook-updated', $event)"
        />
      </NotebookListRow>
      <NotebookListRow
        v-else-if="item.type === 'subscribedNotebook'"
        :notebook="item.notebook"
      >
        <SubscriptionNoteButtons
          v-if="subscriptionById(item.subscriptionId)"
          :subscription="subscriptionById(item.subscriptionId)!"
          @updated="$emit('refresh')"
        />
        <NotebookButtons
          v-else
          v-bind="{ notebook: item.notebook, user }"
          @notebook-updated="$emit('notebook-updated', $event)"
        />
      </NotebookListRow>
      <NotebookCatalogGroupPanel
        v-else
        :group="item"
        :layout="layout"
        :subscriptions="subscriptions"
        :user="user"
        :member-preview-limit="memberPreviewLimit"
        :catalog-filter-active="catalogFilterActive"
        compact-members
        header-navigates-to-group
        @notebook-updated="$emit('notebook-updated', $event)"
        @refresh="$emit('refresh')"
      />
    </template>
  </div>
  <div
    v-else
    class="notebook-catalog-section notebook-catalog-section--grid daisy-grid daisy-grid-cols-1 sm:daisy-grid-cols-2 md:daisy-grid-cols-2 lg:daisy-grid-cols-3 xl:daisy-grid-cols-4 daisy-gap-4"
  >
    <template v-for="item in catalogItems" :key="catalogItemKey(item)">
      <div
        v-if="item.type === 'notebook'"
        role="card"
        class="daisy-card"
        data-cy="notebook-card"
      >
        <NotebookCard :notebook="item.notebook">
          <template #cardHeader>
            <span class="daisy-flex daisy-justify-end daisy-p-0">
              <NotebookButtons
                v-bind="{ notebook: item.notebook, user }"
                @notebook-updated="$emit('notebook-updated', $event)"
              />
            </span>
          </template>
        </NotebookCard>
      </div>
      <div
        v-else-if="item.type === 'subscribedNotebook'"
        role="card"
        class="daisy-card"
        data-cy="notebook-card"
      >
        <NotebookCard :notebook="item.notebook">
          <template #cardHeader>
            <span class="daisy-flex daisy-justify-end daisy-p-0">
              <SubscriptionNoteButtons
                v-if="subscriptionById(item.subscriptionId)"
                :subscription="subscriptionById(item.subscriptionId)!"
                @updated="$emit('refresh')"
              />
              <NotebookButtons
                v-else
                v-bind="{ notebook: item.notebook, user }"
                @notebook-updated="$emit('notebook-updated', $event)"
              />
            </span>
          </template>
        </NotebookCard>
      </div>
      <NotebookCatalogGroupPanel
        v-else
        :group="item"
        :layout="layout"
        :subscriptions="subscriptions"
        :user="user"
        :member-preview-limit="memberPreviewLimit"
        :catalog-filter-active="catalogFilterActive"
        compact-members
        header-navigates-to-group
        @notebook-updated="$emit('notebook-updated', $event)"
        @refresh="$emit('refresh')"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NotebookCatalogEntry } from "./patchNotebookInCatalogItems"
import type {
  Notebook,
  Subscription,
  User,
} from "@generated/doughnut-backend-api"
import NotebookButtons from "./NotebookButtons.vue"
import NotebookCard from "../notebooks/NotebookCard.vue"
import NotebookCatalogGroupPanel from "./NotebookCatalogGroupPanel.vue"
import NotebookListRow from "./NotebookListRow.vue"
import SubscriptionNoteButtons from "../subscriptions/SubscriptionNoteButtons.vue"

const props = defineProps({
  catalogItems: {
    type: Array as PropType<NotebookCatalogEntry[]>,
    required: true,
  },
  subscriptions: {
    type: Array as PropType<Subscription[]>,
    required: true,
  },
  layout: {
    type: String as PropType<"list" | "grid">,
    required: true,
  },
  user: {
    type: Object as PropType<User>,
    required: true,
  },
  catalogFilterActive: {
    type: Boolean,
    default: false,
  },
  memberPreviewLimit: {
    type: Number as PropType<number | null>,
    default: 3,
  },
})

defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
  (e: "refresh"): void
}>()

function subscriptionById(subscriptionId: number): Subscription | undefined {
  return props.subscriptions.find((s) => s.id === subscriptionId)
}

function catalogItemKey(item: NotebookCatalogEntry): string {
  if (item.type === "notebookGroup") return `grp-${item.id}`
  if (item.type === "subscribedNotebook") return `sub-${item.notebook.id}`
  return `nb-${item.notebook.id}`
}
</script>

<style scoped>
.daisy-card {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.daisy-card :deep(.notebook-card) {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
}

.daisy-card :deep(.notebook-card > a) {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>
