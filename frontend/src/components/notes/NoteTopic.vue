<template>
  {{ topic }}
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    parentNote: { type: Object as PropType<Generated.Note> },
  },
  computed: {
    topic() {
      if (!this.parentNote) return this.note.topicConstructor;
      return this.note.topicConstructor.replace(
        /%P/g,
        `[${this.parentNote.topicConstructor}]`,
      );
    },
  },
});
</script>
