<template>
  <div class="text-sm daisy-breadcrumbs max-w-full">
    <ul class="m-0 pl-0">
      <slot name="topLink" />
      <li
        v-for="segment in folderSegments"
        :key="'folder-' + segment.id"
      >
        <router-link
          v-if="breadcrumbNotebookId != null"
          class="inline-flex items-center gap-1 text-base-content"
          :to="folderPageTo(segment.id)"
        >
          <FolderIcon class="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          {{ segment.name }}
        </router-link>
        <span v-else class="inline-flex items-center gap-1 text-base-content">
          <FolderIcon class="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          {{ segment.name }}
        </span>
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
import { Folder as FolderIcon } from "lucide-vue-next"

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
