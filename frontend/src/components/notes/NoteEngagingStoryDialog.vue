<template>
  <h1>Have fun reading this engaging story</h1>
  <form>
    <div class="engaging-story">{{ engagingStory }}</div>
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
    selectedNote: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["done"],
  data() {
    return {
      engagingStory: "Coming soon.",
    };
  },
  mounted() {
    this.api.ai.askAiEngagingStories(this.selectedNote.id).then((res) => {
      this.engagingStory = res.engagingStory;
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
.dialog-buttons
  display: flex
  column-gap: 10px
  margin: 10px 0
</style>
