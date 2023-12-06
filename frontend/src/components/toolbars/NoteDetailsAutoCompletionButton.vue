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
      completionInProgress: undefined as undefined | Generated.AiCompletion,
    };
  },
  methods: {
    initialAutoCompleteDetails() {
      return this.autoCompleteDetails([]);
    },
    async autoCompleteDetails(
      clarifyingQuestionAndAnswers: Generated.ClarifyingQuestionAndAnswer[],
    ) {
      const response = await this.api.ai.askAiCompletion(this.note.id, {
        detailsToComplete: this.note.details,
        clarifyingQuestionAndAnswers,
      });

      if (this.isUnmounted) return;

      if (response.question) {
        this.completionInProgress = response;
        return;
      }

      this.storageAccessor.storedApi().updateTextContent(
        this.note.id,
        {
          topic: this.note.topic,
          details: response.moreCompleteContent,
        },
        () => {},
      );
    },
    clarifyingQuestionAndAnswered(
      clarifyingQuestionAndAnswer: Generated.ClarifyingQuestionAndAnswer,
    ) {
      this.autoCompleteDetails([
        ...(this.completionInProgress?.clarifyingHistory ?? []),
        clarifyingQuestionAndAnswer,
      ]);
      this.completionInProgress = undefined;
    },
  },
  unmounted() {
    this.isUnmounted = true;
  },
});
</script>
