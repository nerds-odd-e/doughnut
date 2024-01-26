<template>
  <h2>How do you like this image from DALL-E?</h2>
  <form>
    <TextInput v-model="prompt" field="prompt" :errors="promptError" />
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
    note: { type: Object as PropType<Generated.Note>, required: true },
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
      prompt: this.note.topic,
      b64Json: undefined as string | undefined,
      promptError: undefined as string | undefined,
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
          await this.api.ai.generateImage(this.prompt)
        ).b64encoded;
      } catch (_) {
        this.promptError = "There is a problem";
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
