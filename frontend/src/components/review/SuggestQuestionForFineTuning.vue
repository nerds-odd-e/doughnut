<template>
  <h2>Suggest This Question For AI Fine Tuning</h2>
  <p>
    <i
      >Sending this question for fine tuning the question generation model will
      make this note and question visible to admin. Are you sure?</i
    >
  </p>
  <textarea
    name="commentField"
    v-model="comment"
    placeholder="Add a comment about the question"
  ></textarea
  ><br />
  <textarea
    name="suggestedQuestionText"
    v-model="suggestedQuestionText"
    placeholder="Add a suggested question"
  ></textarea
  ><br />
  <textarea
    name="suggestedchoice"
    placeholder="Add a suggested choice"
  ></textarea
  ><br />
  <textarea
    name="suggestedcorrect_choice"
    placeholder="Add a suggested correct choice"
  ></textarea>
  <button class="btn btn-success" @click="suggestQuestionForFineTuning">
    OK
  </button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import asPopup from "../commons/Popups/asPopup";

export default defineComponent({
  inheritAttrs: false,
  setup() {
    return { ...useLoadingApi(), ...asPopup() };
  },
  props: {
    quizQuestion: {
      type: Object as PropType<Generated.QuizQuestion>,
      required: true,
    },
  },
  data() {
    return {
      suggestedQuestionText: "" as string,
      comment: "" as string,
    };
  },
  methods: {
    async suggestQuestionForFineTuning() {
      await this.api.reviewMethods.suggestQuestionForFineTuning(
        this.quizQuestion.quizQuestionId,
        {
          comment: this.comment,
          suggestion: this.suggestedQuestionText,
        },
      );
      this.popup.done(null);
    },
  },
});
</script>
