<template>
  <h1>Have fun reading this engaging story</h1>
  <form>
    <div class="engaging-story" :class="{ error: engagingStoryInError }">
      {{ engagingStory }}
    </div>
    <div class="dialog-buttons">
      <input
        @click.once="handleClose"
        type="button"
        value="Close"
        class="btn btn-primary"
      />
    </div>
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import type { StorageAccessor } from "@/store/createNoteStorage";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    selectedNote: { type: Object as PropType<Generated.Note>, required: false },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: false,
    },
  },
  emits: ["done"],
  data() {
    return {
      engagingStory: "In progress... waiting for engaging story",
      engagingStoryInError: false,
    };
  },
  mounted() {
    const request = this.selectedNote
      ? this.api.ai.askAiEngagingStories(this.selectedNote.id)
      : this.api.ai.askAiReviewEngagingStory();

    request
      .then((res) => {
        this.engagingStoryInError = false;
        this.engagingStory = res.engagingStory;
      })
      .catch(() => {
        this.engagingStoryInError = true;
        this.engagingStory = "There is a problem";
      });
  },
  methods: {
    handleClose() {
      this.$emit("done");
    },
  },
});
</script>

<style lang="sass" scoped>
.engaging-story
  max-height: 35vh
  overflow-y: auto
  white-space: pre-wrap
.engaging-story.error
  color: red
.dialog-buttons
  display: flex
  column-gap: 10px
  margin: 10px 0
</style>
