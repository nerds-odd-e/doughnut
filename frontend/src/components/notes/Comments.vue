<template>
  <div class="comments" @click="fetchComments">
    <div class="comment" v-for="comment in comments" :key="comment.id">
      <label>{{ comment }}</label>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: false });
  },
  data() {
    return {
      comments: [] as Generated.Comment[],
    };
  },
  props: {
    noteId: { type: Number, required: true },
  },
  methods: {
    async fetchComments() {
      this.comments = await this.api.getNoteComments(this.noteId);
    },
  },
});
</script>
