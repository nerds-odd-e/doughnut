<template>
  <div class="daisy-text-sm daisy-breadcrumbs daisy-max-w-full">
    <ul class="daisy-m-0 daisy-pl-0">
      <slot name="topLink" />
      <li
        v-for="segment in folderSegments"
        :key="'folder-' + segment.id"
      >
        <router-link
          v-if="breadcrumbNotebookId != null"
          class="daisy-text-base-content"
          :to="folderPageTo(segment.id)"
        >
          {{ segment.name }}
        </router-link>
        <span v-else class="daisy-text-base-content">{{ segment.name }}</span>
      </li>
      <li v-if="$slots.additional">
        <slot name="additional" />
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Folder } from "@generated/doughnut-backend-api"

const props = defineProps({
  folderSegments: {
    type: Array as PropType<Folder[]>,
    default: () => [],
  },
  /** When set, each folder segment links to that folder's container page. */
  breadcrumbNotebookId: {
    type: Number as PropType<number | undefined>,
    default: undefined,
  },
})

const folderPageTo = (folderId: number) => ({
  name: "folderPage" as const,
  params: {
    notebookId: String(props.breadcrumbNotebookId),
    folderId: String(folderId),
  },
})
</script>
