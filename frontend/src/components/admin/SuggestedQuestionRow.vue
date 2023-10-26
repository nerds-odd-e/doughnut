<template>
  <tr>
    <td>{{ suggestedQuestion.preservedQuestion.stem }}</td>
    <td>{{ suggestedQuestion.positiveFeedback ? "Positive" : "Negative" }}</td>
    <td>{{ suggestedQuestion.comment }}</td>
    <td>
      <div class="btn-group" role="group">
        <button
          v-if="!suggestedQuestion.positiveFeedback"
          class="btn btn-sm"
          @click="duplicateQuestion(suggestedQuestion)"
        >
          Duplicate
        </button>
        <button class="btn btn-sm" @click="chatStarter">Chat</button>
        <button class="btn btn-sm" @click="chatStarter">Del</button>
      </div>
    </td>
  </tr>
</template>

<script lang="ts">
import { PropType } from "vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import usePopups from "../commons/Popups/usePopups";

export default {
  setup() {
    return { ...useLoadingApi(), ...usePopups() };
  },
  props: {
    suggestedQuestion: {
      type: Object as PropType<Generated.SuggestedQuestionForFineTuning>,
      required: true,
    },
  },
  emits: ["duplicated"],
  computed: {
    chatStarterMessage() {
      return `In the personal knowledge management system I am using, my notes represents atomic knowledge point and are organized in hirachical structure.
      The system helps me to remember and understand my notes better by asking me questions without revealing the note to me.
      I'm expect the question to be:
      1. A multiple-choice question based on the note in the current context path with only 1 correct answer.
      2. I should be able to answer the question without looking at the note.
      3. The question should be only about the note.
      Please assume the role of a Memory Assistant, and help me with the follow note content and the question generated for it.

      """
      ${this.suggestedQuestion.preservedNoteContent}
      """

      The question I got from the system was:
      ${JSON.stringify(this.suggestedQuestion.preservedQuestion)}

      `;
    },
  },
  methods: {
    async duplicateQuestion(
      suggested: Generated.SuggestedQuestionForFineTuning,
    ) {
      const duplicated =
        await this.api.fineTuning.duplicateSuggestedQuestionForFineTuning(
          suggested.id,
        );
      this.$emit("duplicated", duplicated);
    },
    chatStarter() {
      this.popups.alert(this.chatStarterMessage);
    },
  },
};
</script>
