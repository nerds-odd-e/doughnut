<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: noteTopic.id } }"
    class="text-decoration-none"
  >
    <NoteTopicComponent v-if="!iconized" v-bind="{ noteTopic }" />
    <span v-else>🔗</span>
  </router-link>
</template>

<script setup lang="ts">
import { PropType } from "vue";
import { NoteTopic } from "@/generated/backend";
import NoteTopicComponent from "./core/NoteTopicComponent.vue";

defineProps({
  noteTopic: { type: Object as PropType<NoteTopic>, required: true },
  iconized: { type: Boolean, required: false },
});
</script>
