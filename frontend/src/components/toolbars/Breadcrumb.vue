<template>
  <BasicBreadcrumb
    :folder-segments="ancestorFolders"
    :breadcrumb-notebook-id="breadcrumbNotebookId"
  >
    <template #topLink>
      <slot name="topLink" />
    </template>
    <template #additional>
      <slot name="additional" />
    </template>
  </BasicBreadcrumb>
</template>

<script setup lang="ts">
import type { Folder } from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import BasicBreadcrumb from "@/components/commons/BasicBreadcrumb.vue"

defineProps({
  ancestorFolders: {
    type: Array as PropType<Folder[]>,
    default: () => [],
  },
  breadcrumbNotebookId: {
    type: Number as PropType<number | undefined>,
    default: undefined,
  },
})
</script>
