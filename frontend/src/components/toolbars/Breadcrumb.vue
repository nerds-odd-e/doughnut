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

const props = defineProps({
  noteTopic: {
    type: Object as PropType<NoteTopic>,
    required: true,
  },
})

const ancestors = computed(() => {
  const result: NoteTopic[] = []
  let currentColor = props.noteTopic
  while (currentColor.parentNoteTopic) {
    result.unshift(currentColor.parentNoteTopic)
    currentColor = currentColor.parentNoteTopic
  }
  return result
})
</script>
