<template>
  <a
    :title="'auto-complete details'"
    class="btn btn-sm"
    role="button"
    @click="initialAutoCompleteDetails"
  >
    <SvgRobot />
    <Modal
      v-if="completionInProgress"
      @close_request="completionInProgress = undefined"
    >
      <template #body>
        <AIClarifyingQuestionDialog
          :completion-in-progress="completionInProgress"
          :clarifying-history="clarifyingHistory"
          @submit="clarifyingQuestionAnswered"
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
      completionInProgress: undefined as
        | undefined
        | Generated.AiCompletionResponse,
      clarifyingHistory: [] as Generated.ClarifyingQuestionAndAnswer[],
    };
  },
  methods: {
    async initialAutoCompleteDetails() {
      const response = await this.api.ai.askAiCompletion(this.note.id, {
        detailsToComplete: this.note.details,
      });

      return this.autoCompleteDetails(response);
    },
    async autoCompleteDetails(response: Generated.AiCompletionResponse) {
      if (this.isUnmounted) return;

      if (response.clarifyingQuestionRequiredAction) {
        this.completionInProgress = response;
        return;
      }

      this.completionInProgress = undefined;
      this.storageAccessor.storedApi().updateTextContent(this.note.id, {
        topic: this.note.topic,
        details: response.moreCompleteContent,
      });
    },
    async clarifyingQuestionAnswered(
      clarifyingQuestionAndAnswer: Generated.ClarifyingQuestionAndAnswer,
    ) {
      this.clarifyingHistory.push(clarifyingQuestionAndAnswer);
      const response = await this.api.ai.answerCompletionClarifyingQuestion({
        detailsToComplete: this.note.details,
        answer: clarifyingQuestionAndAnswer.answerFromUser,
        threadId: this.completionInProgress!.threadId,
        runId: this.completionInProgress!.runId,
        toolCallId:
          this.completionInProgress!.clarifyingQuestionRequiredAction
            .toolCallId,
      });
      await this.autoCompleteDetails(response);
    },
  },
  unmounted() {
    this.isUnmounted = true;
  },
});
</script>
