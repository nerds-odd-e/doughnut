<template>
  <a
    :title="'suggest details'"
    class="btn btn-sm"
    role="button"
    @click="
      suggestDetails({
        detailsToComplete: selectedNote.details,
        clarifyingQuestionAndAnswers: [],
      })
    "
  >
    <SvgRobot />
    <Popup1 v-if="aiCompletion" @popup-done="aiCompletion = undefined">
      <AIClarifyingQuestionDialog
        :ai-completion="aiCompletion"
        @submit="
          (clarificationAnswer) =>
            suggestDetails({
              detailsToComplete: selectedNote.details,
              clarifyingQuestionAndAnswers: [
                {
                  questionFromAI: aiCompletion?.question,
                  answerFromUser: clarificationAnswer,
                },
              ],
            })
        "
      />
    </Popup1>
  </a>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import SvgRobot from "../svgs/SvgRobot.vue";
import AIClarifyingQuestionDialog from "../notes/AIClarifyingQuestionDialog.vue";
import Popup1 from "../commons/Popups/Popup1.vue";

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
    Popup1,
    AIClarifyingQuestionDialog,
  },
  data() {
    return {
      isUnmounted: false,
      aiCompletion: undefined as undefined | Generated.AiCompletion,
    };
  },
  methods: {
    async suggestDetails(data: Generated.AiCompletionParams) {
      const response = await this.api.ai.askAiCompletion(
        this.selectedNote.id,
        data,
      );

      if (this.isUnmounted) return;

      if (response.question) {
        this.aiCompletion = response;
        return;
      }

      this.storageAccessor.api(this.$router).updateTextContent(
        this.selectedNote.id,
        {
          topic: this.selectedNote.topic,
          details: response.moreCompleteContent,
        },
        {
          topic: this.selectedNote.topic,
          details: this.selectedNote.details,
        },
      );
    },
  },
  unmounted() {
    this.isUnmounted = true;
  },
});
</script>
