<template>
  <a
    :title="'suggest description'"
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
import AiAdvisor from "@/models/AiAdvisor";
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
    async suggestDescription() {
      const aiAdvisor = new AiAdvisor(this.selectedNote.textContent);
      const prompt = aiAdvisor.promptWithContext();
      const res = await this.api.ai.askAiSuggestions(
        {
          prompt,
          incompleteAssistantMessage: this.selectedNote.textContent.description,
        },
        this.selectedNote.id
      );

      await this.storageAccessor.api(this.$router).updateTextContent(
        this.selectedNote.id,
        {
          title: this.selectedNote.title,
          description: res.suggestion,
        },
        this.selectedNote.textContent
      );
      if (res.finishReason === "length") {
        await this.suggestDescription();
      }
    },
  },
});
</script>
