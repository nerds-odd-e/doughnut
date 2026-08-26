<template>
  <div :class="['flex items-start gap-2', layout === 'list' ? 'mb-3' : 'mb-4']">
    <component
      :is="headerNavigatesToGroup ? RouterLink : 'div'"
      :data-cy="
        headerNavigatesToGroup
          ? 'notebook-group-header-link'
          : 'notebook-group-header'
      "
      class="min-w-0 flex-1"
      v-bind="
        headerNavigatesToGroup
          ? {
              to: { name: 'notebookGroup', params: { groupId: group.id } },
              class: headerLinkClass,
            }
          : {}
      "
    >
      <div class="flex flex-col gap-0.5">
        <h3
          :class="[
            'm-0 font-semibold text-base-content',
            layout === 'list' ? 'text-base' : 'text-lg',
          ]"
        >
          {{ group.name }}
        </h3>
        <p class="m-0 text-sm text-base-content/65">
          {{ subtitle }}
        </p>
      </div>
    </component>
    <NotebookCatalogGroupActions :group="group" />
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { RouterLink } from "vue-router"
import type { NotebookCatalogGroupItem } from "@generated/donut-backend-api"
import NotebookCatalogGroupActions from "./NotebookCatalogGroupActions.vue"

defineProps({
  group: {
    type: Object as PropType<NotebookCatalogGroupItem>,
    required: true,
  },
  subtitle: {
    type: String,
    required: true,
  },
  layout: {
    type: String as PropType<"list" | "grid">,
    required: true,
  },
  headerNavigatesToGroup: {
    type: Boolean,
    default: false,
  },
})

const headerLinkClass =
  "block rounded-md no-underline text-inherit outline-offset-2 hover:bg-primary/10"
</script>
