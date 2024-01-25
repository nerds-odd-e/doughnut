<template>
  {{ topic }}
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
