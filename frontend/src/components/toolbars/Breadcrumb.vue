<template>
  <BasicBreadcrumb v-bind="{ ancestors }">
    <template #topLink>
      <slot name="topLink" />
    </template>
  </BasicBreadcrumb>
</template>

<script setup lang="ts">
import { PropType, computed } from "vue";
import { NoteTopic } from "@/generated/backend";
import BasicBreadcrumb from "../commons/BasicBreadcrumb.vue";

const props = defineProps({
  noteTopic: {
    type: Object as PropType<NoteTopic>,
    required: true,
  },
});

const ancestors = computed(() => {
  const result: NoteTopic[] = [];
  let currentColor = props.noteTopic;
  while (currentColor.parentNoteTopic) {
    result.unshift(currentColor.parentNoteTopic);
    currentColor = currentColor.parentNoteTopic;
  }
  return result;
});
</script>
