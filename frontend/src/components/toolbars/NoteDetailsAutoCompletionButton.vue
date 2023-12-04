<template>
  <a
    :title="'auto-complete details'"
    class="btn btn-sm"
    role="button"
    @click="
      autoCompleteDetails({
        detailsToComplete: note.details,
        clarifyingQuestionAndAnswers: [],
      })
    "
  >
    <SvgRobot />
    <Modal v-if="aiCompletion" @close_request="aiCompletion = undefined">
      <template #body>
        <AIClarifyingQuestionDialog
          :ai-completion="aiCompletion"
          @submit="clarifyingQuestionAndAnswered"
        />
      </template>
    </Modal>
  </a>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import SvgRobot from "../svgs/SvgRobot.vue";
import AIClarifyingQuestionDialog from "../notes/AIClarifyingQuestionDialog.vue";
import Modal from "../commons/Modal.vue";

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
    note: {
      type: Object as PropType<Generated.Note>,
      required: true,
    },
  },
  components: {
    SvgRobot,
    Modal,
    AIClarifyingQuestionDialog,
  },
  data() {
    return {
      isUnmounted: false,
      aiCompletion: undefined as undefined | Generated.AiCompletion,
    };
  },
  methods: {
    async autoCompleteDetails(data: Generated.AiCompletionParams) {
      const response = await this.api.ai.askAiCompletion(this.note.id, data);

      if (this.isUnmounted) return;

      if (response.question) {
        this.aiCompletion = response;
        return;
      }

      this.storageAccessor.api(this.$router).updateTextContent(
        this.note.id,
        {
          topic: this.note.topic,
          details: response.moreCompleteContent,
        },
        {
          topic: this.note.topic,
          details: this.note.details,
        },
      );
    },
    clarifyingQuestionAndAnswered(clarificationAnswer: string) {
      this.autoCompleteDetails({
        detailsToComplete: this.note.details,
        clarifyingQuestionAndAnswers: [
          ...(this.aiCompletion?.clarifyingHistory ?? []),
          {
            questionFromAI: this.aiCompletion?.question,
            answerFromUser: clarificationAnswer,
          },
        ],
      });
      this.aiCompletion = undefined;
    },
  },
  unmounted() {
    this.isUnmounted = true;
  },
});
</script>
