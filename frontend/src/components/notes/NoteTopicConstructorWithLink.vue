<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: note.id } }"
    class="text-decoration-none"
  >
    {{ note.topicConstructor }}
  </router-link>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
  },
});
</script>
