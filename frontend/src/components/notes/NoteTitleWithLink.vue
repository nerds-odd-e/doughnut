<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: note.id } }"
    class="text-decoration-none"
  >
    {{ topic }}
  </router-link>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
  },
  computed: {
    topic() {
      return this.note.topicConstructor.replace(/%P/g, "[LeSS in Action]");
    },
  },
});
</script>
