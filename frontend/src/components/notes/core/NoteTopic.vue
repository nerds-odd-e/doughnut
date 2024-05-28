<template>
  {{ note.topic }}
</template>

<script setup lang="ts">
import { Note } from "@/generated/backend";
import { PropType } from "vue";

defineProps({
  note: { type: Object as PropType<Note>, required: true },
});
</script>
