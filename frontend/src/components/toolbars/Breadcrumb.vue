<template>
  <BasicBreadcrumb v-bind="{ ancestors }">
    <template #topLink>
      <slot name="topLink" />
    </template>
  </BasicBreadcrumb>
</template>

<script setup lang="ts">
import { NoteTopology } from "@generated/backend"
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
})

const ancestors = computed(() => {
  const result: NoteTopology[] = []
  let currentTopology = props.noteTopology

  if (props.includingSelf) {
    result.push(currentTopology)
  }

  while (currentTopology.parentOrSubjectNoteTopology) {
    result.unshift(currentTopology.parentOrSubjectNoteTopology)
    currentTopology = currentTopology.parentOrSubjectNoteTopology
  }
  return result
})
</script>
