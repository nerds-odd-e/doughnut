<template>
  <div class="comment" v-for="comment in comments" :key="comment.id">
    {{ comment.content }}
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    comments: { type: Object as PropType<Generated.Comment[]>, required: true },
  },
});
</script>
