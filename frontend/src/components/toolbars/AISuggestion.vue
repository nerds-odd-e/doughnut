<template>
  <a
    :title="'Suggest1'"
    class="btn btn-sm"
    role="button"
    @click="suggestDescription"
  >
    <SvgRobot />
  </a>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import SvgRobot from "../svgs/SvgRobot.vue";

export default defineComponent({
  setup() {
    return {
      ...useLoadingApi(),
    };
  },
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    selectedNote: {
      type: Object as PropType<Generated.Note>,
      required: true,
    },
  },
  components: {
    SvgRobot,
  },
  methods: {
    async askSuggestionApi(prompt: string) {
      const res = await this.api.ai.askAiSuggestions({
        prompt,
      });

      await this.storageAccessor.api(this.$router).updateTextContent(
        this.selectedNote.id,
        {
          title: this.selectedNote.title,
          description: res.suggestion,
        },
        this.selectedNote.textContent
      );
      if (res.finishReason === "length") {
        await this.askSuggestionApi(res.suggestion);
      }
    },
    async suggestDescription() {
      await this.askSuggestionApi(
        this.selectedNote.textContent.description
          ?.replace(/<\/?[^>]+(>|$)/g, "")
          .trim() || `Tell me about "${this.selectedNote.title}"`
      );
    },
  },
});
</script>
