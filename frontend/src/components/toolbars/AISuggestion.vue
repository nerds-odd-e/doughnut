<template>
  <a
    :title="'suggest description'"
    class="btn btn-sm"
    role="button"
    @click="suggestDescription(selectedNote.textContent.description)"
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
    async keepAskingAISuggestionUntilStop(
      prev?: string,
      earlyReturn?: (suggestion: string) => void
    ): Promise<string> {
      const aiAdvisor = new AiAdvisor(this.selectedNote.textContent);
      const prompt = aiAdvisor.promptWithContext();
      const res = await this.api.ai.askAiSuggestions(
        {
          prompt,
          incompleteAssistantMessage: prev ?? "",
        },
        this.selectedNote.id
      );
      if (earlyReturn) {
        earlyReturn(res.suggestion);
      }
      if (res.finishReason === "length") {
        return this.keepAskingAISuggestionUntilStop(
          res.suggestion,
          earlyReturn
        );
      }
      return res.suggestion;
    },

    async suggestDescription(prev?: string) {
      await this.keepAskingAISuggestionUntilStop(prev, (suggestion) => {
        this.storageAccessor.api(this.$router).updateTextContent(
          this.selectedNote.id,
          {
            title: this.selectedNote.title,
            description: suggestion,
          },
          this.selectedNote.textContent
        );
      });
    },
  },
});
</script>
