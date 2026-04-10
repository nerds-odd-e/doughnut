<template>
  <div
    v-if="layout === 'list'"
    data-cy="notebook-group-card"
    class="notebook-catalog-group daisy-rounded-box daisy-border daisy-border-primary/25 daisy-bg-primary/5 daisy-p-4"
    :aria-label="hint.ariaLabel"
  >
    <component
      :is="headerNavigatesToGroup ? RouterLink : 'div'"
      :data-cy="
        headerNavigatesToGroup
          ? 'notebook-group-header-link'
          : 'notebook-group-header'
      "
      v-bind="
        headerNavigatesToGroup
          ? {
              to: { name: 'notebookGroup', params: { groupId: group.id } },
              class: headerLinkClassList,
            }
          : {}
      "
    >
      <div class="daisy-mb-3 daisy-flex daisy-flex-col daisy-gap-0.5">
        <h3 class="daisy-m-0 daisy-text-base daisy-font-semibold daisy-text-base-content">
          {{ group.name }}
        </h3>
        <p class="daisy-m-0 daisy-text-sm daisy-text-base-content/65">
          {{ hint.subtitle }}
        </p>
      </div>
    </component>
    <div class="daisy-flex daisy-flex-col daisy-gap-2 daisy-border-l-2 daisy-border-primary/30 daisy-pl-3">
      <NotebookListRow
        v-for="nb in previewNotebooks"
        :key="nb.id"
        :notebook="nb"
        :is-subscribed="!!subscriptionForNotebook(nb.id)"
        :compact="compactMembers"
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
  <div
    v-else
    data-cy="notebook-group-card"
    class="notebook-catalog-group daisy-col-span-full daisy-rounded-box daisy-border daisy-border-primary/25 daisy-bg-primary/5 daisy-p-4"
    :aria-label="hint.ariaLabel"
  >
    <component
      :is="headerNavigatesToGroup ? RouterLink : 'div'"
      :data-cy="
        headerNavigatesToGroup
          ? 'notebook-group-header-link'
          : 'notebook-group-header'
      "
      v-bind="
        headerNavigatesToGroup
          ? {
              to: { name: 'notebookGroup', params: { groupId: group.id } },
              class: headerLinkClassList,
            }
          : {}
      "
    >
      <div class="daisy-mb-4 daisy-flex daisy-flex-col daisy-gap-0.5">
        <h3 class="daisy-m-0 daisy-text-lg daisy-font-semibold daisy-text-base-content">
          {{ group.name }}
        </h3>
        <p class="daisy-m-0 daisy-text-sm daisy-text-base-content/65">
          {{ hint.subtitle }}
        </p>
      </div>
    </component>
    <div
      class="daisy-grid daisy-grid-cols-1 sm:daisy-grid-cols-2 md:daisy-grid-cols-2 lg:daisy-grid-cols-3 xl:daisy-grid-cols-4 daisy-gap-4"
    >
      <div
        v-for="nb in previewNotebooks"
        :key="nb.id"
        role="card"
        class="daisy-card"
        :class="{ 'subscribed-notebook': !!subscriptionForNotebook(nb.id) }"
        data-cy="notebook-card"
      >
        <NotebookCard :notebook="nb" :compact="compactMembers">
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

<script setup lang="ts">
import type { PropType } from "vue"
import { computed } from "vue"
import { RouterLink } from "vue-router"
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
import { groupCatalogMemberPreviewHint } from "./groupMemberHint"

const props = defineProps({
  group: {
    type: Object as PropType<NotebookCatalogGroupItem>,
    required: true,
  },
  layout: {
    type: String as PropType<"list" | "grid">,
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
  headerNavigatesToGroup: {
    type: Boolean,
    default: false,
  },
  memberPreviewLimit: {
    type: Number as PropType<number | null>,
    default: null,
  },
  catalogFilterActive: {
    type: Boolean,
    default: false,
  },
  compactMembers: {
    type: Boolean,
    default: false,
  },
})

defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
  (e: "refresh"): void
}>()

const headerLinkClassList =
  "daisy-block daisy-rounded-md daisy-no-underline daisy-text-inherit daisy-outline-offset-2 hover:daisy-bg-primary/10"

const previewNotebooks = computed(() => {
  const all = props.group.notebooks
  const lim = props.memberPreviewLimit
  if (lim === null) {
    return all
  }
  return all.slice(0, lim)
})

const hint = computed(() =>
  groupCatalogMemberPreviewHint({
    groupName: props.group.name,
    notebooks: props.group.notebooks,
    memberPreviewLimit: props.memberPreviewLimit,
    catalogFilterActive: props.catalogFilterActive,
  })
)

function subscriptionForNotebook(notebookId: number): Subscription | undefined {
  return props.subscriptions.find((s) => s.notebook?.id === notebookId)
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
