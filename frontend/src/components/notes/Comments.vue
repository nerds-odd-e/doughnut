<template>
  <button v-if="featureToggle" class="comments" @click="fetchComments">
    toggle comments
  </button>
  <div class="comment" v-for="comment in comments" :key="comment.id">
    {{ comment.content }}
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: false });
  },
  data() {
    return {
      comments: [] as Generated.Comment[],
    };
  },
  props: {
    noteId: { type: Number, required: true },
  },
  computed: {
    featureToggle() {
      return this.piniaStore.featureToggle;
    },
  },
  methods: {
    async fetchComments() {
      this.comments = await this.api.getNoteComments(this.noteId);
    },
  },
});
</script>
