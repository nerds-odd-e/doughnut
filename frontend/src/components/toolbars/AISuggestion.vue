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
  data() {
    return {
      isUnmounted: false,
    };
  },
  methods: {
    async keepAskingAISuggestionUntilStop(
      prompt: string,
      noteId: Doughnut.ID,
      prev?: string,
      interimResultShouldContinue?: (suggestion: string) => boolean
    ): Promise<string> {
      const res = await this.api.ai.askAiSuggestions(
        {
          prompt,
          incompleteAssistantMessage: prev ?? "",
        },
        noteId
      );
      if (interimResultShouldContinue) {
        if (!interimResultShouldContinue(res.suggestion)) return res.suggestion;
      }
      if (res.finishReason === "length") {
        return this.keepAskingAISuggestionUntilStop(
          prompt,
          noteId,
          res.suggestion,
          interimResultShouldContinue
        );
      }
      return res.suggestion;
    },

    async suggestDescription(prev?: string) {
      const aiAdvisor = new AiAdvisor(this.selectedNote.textContent);
      const prompt = aiAdvisor.promptWithContext();
      await this.keepAskingAISuggestionUntilStop(
        prompt,
        this.selectedNote.id,
        prev,
        (suggestion) => {
          this.storageAccessor.api(this.$router).updateTextContent(
            this.selectedNote.id,
            {
              title: this.selectedNote.title,
              description: suggestion,
            },
            this.selectedNote.textContent
          );
          return !this.isUnmounted;
        }
      );
    },
  },
  unmounted() {
    this.isUnmounted = true;
  },
});
</script>
