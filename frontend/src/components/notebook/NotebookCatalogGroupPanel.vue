<template>
  <div
    v-if="layout === 'list'"
    data-cy="notebook-group-card"
    class="notebook-catalog-group rounded-box border border-primary/25 bg-primary/5 p-4"
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
      <div class="mb-3 flex flex-col gap-0.5">
        <h3 class="m-0 text-base font-semibold text-base-content">
          {{ group.name }}
        </h3>
        <p class="m-0 text-sm text-base-content/65">
          {{ hint.subtitle }}
        </p>
      </div>
    </component>
    <div class="flex flex-col gap-2 border-l-2 border-primary/30 pl-3">
      <NotebookListRow
        v-for="nb in previewNotebooks"
        :key="nb.notebook.id"
        :notebook="nb.notebook"
        :compact="compactMembers"
      >
        <SubscriptionNoteButtons
          v-if="subscriptionForNotebook(nb.notebook.id)"
          :subscription="subscriptionForNotebook(nb.notebook.id)!"
          :notebook-id="nb.notebook.id"
          :catalog-group-id="group.id"
          @updated="$emit('refresh')"
        />
        <NotebookButtons
          v-else
          v-bind="{ notebook: nb.notebook, user }"
          :has-attached-book="nb.hasAttachedBook"
          :catalog-group-id="group.id"
          @notebook-updated="$emit('notebook-updated', $event)"
          @refresh="$emit('refresh')"
        />
      </NotebookListRow>
    </div>
  </div>
  <div
    v-else
    data-cy="notebook-group-card"
    class="notebook-catalog-group col-span-full rounded-box border border-primary/25 bg-primary/5 p-4"
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
      <div class="mb-4 flex flex-col gap-0.5">
        <h3 class="m-0 text-lg font-semibold text-base-content">
          {{ group.name }}
        </h3>
        <p class="m-0 text-sm text-base-content/65">
          {{ hint.subtitle }}
        </p>
      </div>
    </component>
    <div
      class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4"
    >
      <div
        v-for="nb in previewNotebooks"
        :key="nb.notebook.id"
        role="card"
        class="daisy-card"
        data-cy="notebook-card"
      >
        <NotebookCard :notebook="nb.notebook" :compact="compactMembers">
          <template #cardHeader>
            <span class="flex justify-end p-0">
              <SubscriptionNoteButtons
                v-if="subscriptionForNotebook(nb.notebook.id)"
                :subscription="subscriptionForNotebook(nb.notebook.id)!"
                :notebook-id="nb.notebook.id"
                :catalog-group-id="group.id"
                @updated="$emit('refresh')"
              />
              <NotebookButtons
                v-else
                v-bind="{ notebook: nb.notebook, user }"
                :has-attached-book="nb.hasAttachedBook"
                :catalog-group-id="group.id"
                @notebook-updated="$emit('notebook-updated', $event)"
                @refresh="$emit('refresh')"
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
  SubscriptionForNotebooksListing,
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
    type: Array as PropType<SubscriptionForNotebooksListing[]>,
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
  "block rounded-md no-underline text-inherit outline-offset-2 hover:bg-primary/10"

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

function subscriptionForNotebook(
  notebookId: number
): SubscriptionForNotebooksListing | undefined {
  return props.subscriptions.find((s) => s.notebook.id === notebookId)
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
