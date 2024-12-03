<template>
  <BasicBreadcrumb v-bind="{ ancestors }">
    <template #topLink>
      <slot name="topLink" />
    </template>
  </BasicBreadcrumb>
</template>

<script setup lang="ts">
import { NoteTopic } from "@/generated/backend"
import type { PropType } from "vue"
import { computed } from "vue"
import BasicBreadcrumb from "@/components/commons/BasicBreadcrumb.vue"

const props = defineProps({
  noteTopic: {
    type: Object as PropType<NoteTopic>,
    required: true,
  },
  includingSelf: {
    type: Boolean,
    default: false,
  },
})

const ancestors = computed(() => {
  const result: NoteTopic[] = []
  let currentTopic = props.noteTopic

  if (props.includingSelf) {
    result.push(currentTopic)
  }

  while (currentTopic.parentNoteTopic) {
    result.unshift(currentTopic.parentNoteTopic)
    currentTopic = currentTopic.parentNoteTopic
  }
  return result
})
</script>
