<template>
  <template v-if="note.targetForTopic">
    <span>
      {{ note.topicConstructor.substring(1) }}
    </span>
    &nbsp;
    <span>
      {{ note.targetForTopic.topicConstructor }}
    </span>
  </template>
  <template v-else>
    {{ note.topic }}
  </template>
</template>

<script setup lang="ts">
import { Note } from "@/generated/backend";
import { PropType } from "vue";

defineProps({
  note: { type: Object as PropType<Note>, required: true },
});
</script>
