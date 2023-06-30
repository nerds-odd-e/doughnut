<template>
  <h2>Have fun reading this engaging story</h2>
  <form>
    <TextInput
      v-model="engagingStory"
      field="engagingStory"
      :errors="engagingStoryInError"
    />
    <div>
      <img class="ai-art" v-if="imageSrc" :src="imageSrc" />
    </div>
  </form>
  <button class="btn btn-secondary" @click="askForImage">Ask again</button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import type { StorageAccessor } from "@/store/createNoteStorage";
import useLoadingApi from "../../managedApi/useLoadingApi";
import TextInput from "../form/TextInput.vue";

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
    TextInput,
  },
  data() {
    return {
      engagingStory: this.selectedNote.title,
      b64Json: undefined as string | undefined,
      engagingStoryInError: undefined as string | undefined,
    };
  },
  computed: {
    imageSrc() {
      if (!this.b64Json) {
        return undefined;
      }
      return `data:image/png;base64,${this.b64Json}`;
    },
  },
  methods: {
    async askForImage() {
      try {
        this.b64Json = (
          await this.api.ai.askAiEngagingStories(this.engagingStory)
        ).b64encoded;
      } catch (_) {
        this.engagingStoryInError = "There is a problem";
      }
    },
  },
  mounted() {
    this.askForImage();
  },
});
</script>

<style lang="scss" scoped>
.ai-art {
  width: 100%;
  height: 100%;
}
</style>
