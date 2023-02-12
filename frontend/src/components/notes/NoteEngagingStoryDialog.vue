<template>
  <h1>Have fun reading this engaging story</h1>
  <form>
    <TextArea
      v-model="engagingStory"
      field="engagingStory"
      :errors="engagingStoryInError"
    />
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import type { StorageAccessor } from "@/store/createNoteStorage";
import TextArea from "../form/TextArea.vue";
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
  components: {
    TextArea,
  },
  data() {
    return {
      engagingStory: "In progress... waiting for engaging story",
      engagingStoryInError: undefined as string | undefined,
    };
  },
  mounted() {
    const request = this.selectedNote
      ? this.api.ai.askAiEngagingStories(this.selectedNote.id)
      : this.api.ai.askAiReviewEngagingStory();

    request
      .then((res) => {
        this.engagingStory = res.engagingStory;
        console.log(this.engagingStory);
      })
      .catch(() => {
        this.engagingStoryInError = "There is a problem";
      });
  },
});
</script>
