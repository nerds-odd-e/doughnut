<template>
  <BasicBreadcrumb :folder-segments="ancestorFolders">
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
})
</script>
