<template>
  <Breadcrumb v-bind="quizQuestion.notebookPosition" />
  <div class="quiz-instruction">
    <ShowPicture
      v-if="quizQuestion.pictureWithMask"
      v-bind="quizQuestion.pictureWithMask"
      :opacity="1"
    />
    <h2 v-if="!!quizQuestion.mainTopic" class="text-center">
      {{ quizQuestion.mainTopic }}
    </h2>
    <div
      style="white-space: pre-wrap"
      v-if="quizQuestion.stem"
      v-html="quizQuestion.stem"
    ></div>

    <div v-if="quizQuestion.questionType === 'SPELLING'">
      <form @submit.prevent.once="submitAnswer({ spellingAnswer: answer })">
        <TextInput
          scope-name="review_point"
          field="answer"
          v-model="answer"
          placeholder="put your answer here"
          v-focus
        />
        <input
          type="submit"
          value="OK"
          class="btn btn-primary btn-lg btn-block"
        />
      </form>
    </div>
    <QuizQuestionChoices
      v-if="quizQuestion.choices"
      :choices="quizQuestion.choices"
      :correct-choice-index="correctChoiceIndex"
      :answer-choice-index="answerChoiceIndex"
      :disabled="disabled"
      @answer="submitAnswer($event)"
    />
    <div class="mark-question">
      <PopButton
        title="send this question for fine tuning the question generation model"
      >
        <template #button_face>
          <SvgRaiseHand />
        </template>
        <textarea
          name="commentField"
          placeholder="Add a comment about the question"
        ></textarea
        ><br />
        <textarea
          name="suggestedquestion"
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
        <button class="btn btn-success" @click="suggestQuestion">OK</button>
      </PopButton>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ShowPicture from "../notes/ShowPicture.vue";
import TextInput from "../form/TextInput.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import usePopups from "../commons/Popups/usePopups";
import QuizQuestionChoices from "./QuizQuestionChoices.vue";
import Breadcrumb from "../toolbars/Breadcrumb.vue";
import SvgRaiseHand from "../svgs/SvgRaiseHand.vue";
import asPopup from "../commons/Popups/asPopup";
// import { INewRuntimeOptions } from "@cucumber/cucumber";
// import { Input } from "postcss";

export default defineComponent({
  inheritAttrs: false,
  setup() {
    return { ...useLoadingApi(), ...usePopups(), ...asPopup() };
  },
  props: {
    quizQuestion: {
      type: Object as PropType<Generated.QuizQuestion>,
      required: true,
    },
    correctChoiceIndex: Number,
    answerChoiceIndex: Number,
    disabled: Boolean,
  },
  components: {
    ShowPicture,
    TextInput,
    QuizQuestionChoices,
    Breadcrumb,
    SvgRaiseHand,
  },
  emits: ["answered"],
  data() {
    return {
      answer: "" as string,
    };
  },
  methods: {
    async submitAnswer(answerData: Partial<Generated.Answer>) {
      try {
        const answerResult = await this.api.reviewMethods.processAnswer(
          this.quizQuestion.quizQuestionId,
          answerData,
        );
        this.$emit("answered", answerResult);
      } catch (_e) {
        await this.popups.alert(
          "This review point doesn't exist any more or is being skipped now. Moving on to the next review point...",
        );
      }
    },

    async suggestQuestion() {
      await this.api.reviewMethods.markQuestion(
        this.quizQuestion.quizQuestionId,
      );
      this.popup.done(null);
    },
  },
});
</script>

<style lang="scss" scoped>
.quiz-instruction {
  position: relative;
  margin-top: 20px;
}

.mark-question {
  button {
    border: none;
    background: none;
  }
}
</style>
