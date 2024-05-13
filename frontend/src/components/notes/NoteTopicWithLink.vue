<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: note.id } }"
    class="text-decoration-none"
  >
    <NoteTopic v-bind="{ topic: note.topic }" />
  </router-link>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import { Note } from "@/generated/backend";
import NoteTopic from "./core/NoteTopic.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Note>, required: true },
  },
  components: { NoteTopic },
});
</script>
