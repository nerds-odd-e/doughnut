<template>
  <h2>Have fun reading this engaging story</h2>
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
    selectedNote: { type: Object as PropType<Generated.Note>, required: true },
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
      engagingStory: `Draw me a picture, explaining wait is "${this.selectedNote.title}"?`,
      engagingStoryInError: undefined as string | undefined,
    };
  },
  mounted() {
    const request = this.api.ai.askAiEngagingStories(this.engagingStory);

    request
      .then((res) => {
        this.engagingStory = res.engagingStory;
      })
      .catch(() => {
        this.engagingStoryInError = "There is a problem";
      });
  },
});
</script>
