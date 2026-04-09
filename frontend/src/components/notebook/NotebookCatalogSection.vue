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
        :is-subscribed="true"
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
      <div
        v-else
        data-cy="notebook-group-card"
        class="notebook-catalog-group daisy-rounded-box daisy-border daisy-border-primary/25 daisy-bg-primary/5 daisy-p-4"
        :aria-label="hintForGroup(item).ariaLabel"
      >
        <div class="daisy-mb-3 daisy-flex daisy-flex-col daisy-gap-0.5">
          <h3 class="daisy-m-0 daisy-text-base daisy-font-semibold daisy-text-base-content">
            {{ item.name }}
          </h3>
          <p class="daisy-m-0 daisy-text-sm daisy-text-base-content/65">
            {{ hintForGroup(item).subtitle }}
          </p>
        </div>
        <div class="daisy-flex daisy-flex-col daisy-gap-2 daisy-border-l-2 daisy-border-primary/30 daisy-pl-3">
          <NotebookListRow
            v-for="nb in item.notebooks"
            :key="nb.id"
            :notebook="nb"
            :is-subscribed="!!subscriptionForNotebook(nb.id)"
          >
            <SubscriptionNoteButtons
              v-if="subscriptionForNotebook(nb.id)"
              :subscription="subscriptionForNotebook(nb.id)!"
              @updated="$emit('refresh')"
            />
            <NotebookButtons
              v-else
              v-bind="{ notebook: nb, user }"
              @notebook-updated="$emit('notebook-updated', $event)"
            />
          </NotebookListRow>
        </div>
      </div>
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
        class="daisy-card subscribed-notebook"
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
      <div
        v-else
        data-cy="notebook-group-card"
        class="notebook-catalog-group daisy-col-span-full daisy-rounded-box daisy-border daisy-border-primary/25 daisy-bg-primary/5 daisy-p-4"
        :aria-label="hintForGroup(item).ariaLabel"
      >
        <div class="daisy-mb-4 daisy-flex daisy-flex-col daisy-gap-0.5">
          <h3 class="daisy-m-0 daisy-text-lg daisy-font-semibold daisy-text-base-content">
            {{ item.name }}
          </h3>
          <p class="daisy-m-0 daisy-text-sm daisy-text-base-content/65">
            {{ hintForGroup(item).subtitle }}
          </p>
        </div>
        <div
          class="daisy-grid daisy-grid-cols-1 sm:daisy-grid-cols-2 md:daisy-grid-cols-2 lg:daisy-grid-cols-3 xl:daisy-grid-cols-4 daisy-gap-4"
        >
          <div
            v-for="nb in item.notebooks"
            :key="nb.id"
            role="card"
            class="daisy-card"
            :class="{ 'subscribed-notebook': !!subscriptionForNotebook(nb.id) }"
            data-cy="notebook-card"
          >
            <NotebookCard :notebook="nb">
              <template #cardHeader>
                <span class="daisy-flex daisy-justify-end daisy-p-0">
                  <SubscriptionNoteButtons
                    v-if="subscriptionForNotebook(nb.id)"
                    :subscription="subscriptionForNotebook(nb.id)!"
                    @updated="$emit('refresh')"
                  />
                  <NotebookButtons
                    v-else
                    v-bind="{ notebook: nb, user }"
                    @notebook-updated="$emit('notebook-updated', $event)"
                  />
                </span>
              </template>
            </NotebookCard>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NotebookCatalogEntry } from "./patchNotebookInCatalogItems"
import type {
  Notebook,
  NotebookCatalogGroupItem,
  Subscription,
  User,
} from "@generated/doughnut-backend-api"
import NotebookButtons from "./NotebookButtons.vue"
import NotebookCard from "../notebooks/NotebookCard.vue"
import NotebookListRow from "./NotebookListRow.vue"
import SubscriptionNoteButtons from "../subscriptions/SubscriptionNoteButtons.vue"
import { groupMemberHint } from "./groupMemberHint"

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
})

defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
  (e: "refresh"): void
}>()

function subscriptionById(subscriptionId: number): Subscription | undefined {
  return props.subscriptions.find((s) => s.id === subscriptionId)
}

function subscriptionForNotebook(notebookId: number): Subscription | undefined {
  return props.subscriptions.find((s) => s.notebook?.id === notebookId)
}

function catalogItemKey(item: NotebookCatalogEntry): string {
  if (item.type === "notebookGroup") return `grp-${item.id}`
  if (item.type === "subscribedNotebook") return `sub-${item.notebook.id}`
  return `nb-${item.notebook.id}`
}

function hintForGroup(item: NotebookCatalogGroupItem) {
  return groupMemberHint(item.notebooks)
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

.daisy-card.subscribed-notebook :deep(.notebook-card) {
  background: linear-gradient(
    to right,
    oklch(var(--p) / 0.2) 0%,
    oklch(var(--p) / 0.1) 5%
  );
  border: 1px solid oklch(var(--p) / 0.4);
}

.daisy-card.subscribed-notebook :deep(.notebook-binding) {
  background: oklch(var(--p));
  border-right: 1px solid oklch(var(--p) / 0.7);
}
</style>
