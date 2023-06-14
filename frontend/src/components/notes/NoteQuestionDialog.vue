<template>
  <h2>Generate a question</h2>
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
  <button class="btn btn-secondary" @click="generateQuestion">Ask again</button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import type { StorageAccessor } from "@/store/createNoteStorage";
import AiAdvisor from "@/models/AiAdvisor";
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
    async generateQuestion() {
      const aiAdvisor = new AiAdvisor(this.selectedNote.textContent);
      const prompt = aiAdvisor.questionPrompt();
      const res = await this.api.ai.askAiSuggestions({
        prompt,
      });
      if (res.finishReason === "length") {
        await this.generateQuestion();
      }
    },
  },
  mounted() {
    this.generateQuestion();
  },
});
</script>

<style lang="scss" scoped>
.ai-art {
  width: 100%;
  height: 100%;
}
</style>
