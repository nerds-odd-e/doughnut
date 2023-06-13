<template>
  <a
    :title="'generate question'"
    class="btn btn-sm"
    role="button"
    @click="generateQuestion"
  >
    <SvgSad />
  </a>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import AiAdvisor from "@/models/AiAdvisor";
import SvgSad from "../svgs/SvgSad.vue";

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
    async generateQuestion() {
      const aiAdvisor = new AiAdvisor(this.selectedNote.textContent);
      const prompt = aiAdvisor.questionPrompt();
      const res = await this.api.ai.askAiQuestion(prompt);

      await this.storageAccessor.api(this.$router).updateTextContent(
        this.selectedNote.id,
        {
          title: this.selectedNote.title,
          description: aiAdvisor.processResult(res.question),
        },
        this.selectedNote.textContent
      );
      if (res.finishReason === "length") {
        await this.generateQuestion();
      }
    },
  },
});
</script>
