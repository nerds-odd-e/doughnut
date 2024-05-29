<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: noteTopic.id } }"
    class="text-decoration-none"
  >
    <NoteTopicComponent v-bind="{ noteTopic }" />
  </router-link>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import { NoteTopic } from "@/generated/backend";
import NoteTopicComponent from "./core/NoteTopic.vue";

export default defineComponent({
  props: {
    noteTopic: { type: Object as PropType<NoteTopic>, required: true },
  },
  components: { NoteTopicComponent },
});
</script>
