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
import AiAdvicer from "@/models/AiAdvicer";
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
      const aiAdvicer = new AiAdvicer(this.selectedNote.textContent);
      const prompt = aiAdvicer.prompt();
      const res = await this.api.ai.askAiSuggestions({
        prompt,
      });

      await this.storageAccessor.api(this.$router).updateTextContent(
        this.selectedNote.id,
        {
          title: this.selectedNote.title,
          description: aiAdvicer.processResult(res.suggestion),
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
