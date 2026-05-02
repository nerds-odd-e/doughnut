<template>
  <BasicBreadcrumb v-bind="{ ancestors, folderSegments }">
    <template #topLink>
      <slot name="topLink" />
    </template>
  </BasicBreadcrumb>
</template>

<script setup lang="ts">
import type {
  FolderTrailSegment,
  NoteTopology,
} from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import { computed } from "vue"
import BasicBreadcrumb from "@/components/commons/BasicBreadcrumb.vue"

const props = defineProps({
  noteTopology: {
    type: Object as PropType<NoteTopology>,
    required: true,
  },
  includingSelf: {
    type: Boolean,
    default: false,
  },
  ancestorFolders: {
    type: Array as PropType<FolderTrailSegment[]>,
    default: () => [],
  },
})

const folderSegments = computed(() => props.ancestorFolders ?? [])

const ancestors = computed(() => {
  if (folderSegments.value.length > 0) {
    if (props.includingSelf) {
      return [props.noteTopology]
    }
    return []
  }
  return props.includingSelf ? [props.noteTopology] : []
})
</script>
