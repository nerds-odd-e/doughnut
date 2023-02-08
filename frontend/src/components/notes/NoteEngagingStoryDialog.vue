<template>
  <h1>Have fun reading this engaging story</h1>
  <form>
    <TextArea v-model="engagingStory" />
    <div class="dialog-buttons">
      <input type="button" value="Close" class="btn btn-primary" />
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
});
</script>

<style lang="sass" scoped>
.dialog-buttons
  display: flex
  column-gap: 10px
  margin: 10px 0
</style>
