<template>
  <a
    :title="'auto-complete details'"
    class="btn btn-sm"
    role="button"
    @click="initialAutoCompleteDetails"
  >
    <SvgRobot />
    <Modal
      v-if="clarifyingQuestion"
      @close_request="clarifyingQuestion = undefined"
    >
      <template #body>
        <AIClarifyingQuestionDialog
          :clarifying-question="clarifyingQuestion"
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
import {
  AiCompletionResponse,
  ClarifyingQuestion,
  Note,
} from "@/generated/backend";
import ClarifyingQuestionAndAnswer from "@/models/ClarifyingQuestionAndAnswer";
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
      type: Object as PropType<Note>,
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
      threadRespons: undefined as undefined | AiCompletionResponse,
      clarifyingQuestion: undefined as undefined | ClarifyingQuestion,
      clarifyingHistory: [] as ClarifyingQuestionAndAnswer[],
    };
  },
  methods: {
    async initialAutoCompleteDetails() {
      const response = await this.managedApi.restAiController.getCompletion(
        this.note.id,
        {
          detailsToComplete: this.note.details,
        },
      );

      return this.autoCompleteDetails(response);
    },
    async autoCompleteDetails(response: AiCompletionResponse) {
      if (this.isUnmounted) return;

      if (response.requiredAction?.clarifyingQuestion) {
        this.threadRespons = response;
        this.clarifyingQuestion = response.requiredAction.clarifyingQuestion;
        return;
      }

      this.clarifyingQuestion = undefined;
      this.storageAccessor
        .storedApi()
        .updateTextField(
          this.note.id,
          "edit details",
          this.note.details + response.requiredAction!.contentToAppend!,
        );
    },
    async clarifyingQuestionAnswered(
      clarifyingQuestionAndAnswer: ClarifyingQuestionAndAnswer,
    ) {
      this.clarifyingHistory.push(clarifyingQuestionAndAnswer);
      const response =
        await this.managedApi.restAiController.answerCompletionClarifyingQuestion(
          {
            detailsToComplete: this.note.details,
            answer: clarifyingQuestionAndAnswer.answerFromUser,
            threadId: this.threadRespons!.threadId,
            runId: this.threadRespons!.runId,
            toolCallId: this.threadRespons!.requiredAction!.toolCallId,
          },
        );
      await this.autoCompleteDetails(response);
    },
  },
  unmounted() {
    this.isUnmounted = true;
  },
});
</script>
