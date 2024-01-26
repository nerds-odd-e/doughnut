<template>
  {{ note.topic }}
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    parentNote: { type: Object as PropType<Generated.Note> },
  },
});
</script>
