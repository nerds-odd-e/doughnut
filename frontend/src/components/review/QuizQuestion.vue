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
      style="white-space: pre-wrap; margin-right: 60px"
      v-if="quizQuestion.stem"
      v-html="quizQuestion.stem"
    ></div>
    <span class="mark-question">
      <button
        v-if="!markedAsGood"
        class="thumb-up-hollow"
        @click="() => markAsGood()"
      >
        <SvgThumbUpHollow />
      </button>
      <button
        v-if="markedAsGood"
        class="thumb-up-filled"
        @click="() => unmarkAsGood()"
      >
        <SvgThumbUpFilled />
      </button>
      <button
        v-if="!markedAsBad"
        class="thumb-down-hollow"
        @click="() => markAsBad()"
      >
        <SvgThumbDownHollow />
      </button>
      <button
        v-if="markedAsBad"
        class="thumb-down-filled"
        @click="() => unmarkAsBad()"
      >
        <SvgThumbDownFilled />
      </button>
    </span>
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
import SvgThumbUpHollow from "../svgs/SvgThumbUpHollow.vue";
import SvgThumbUpFilled from "../svgs/SvgThumbUpFilled.vue";
import SvgThumbDownHollow from "../svgs/SvgThumbDownHollow.vue";
import SvgThumbDownFilled from "../svgs/SvgThumbDownFilled.vue";

export default defineComponent({
  inheritAttrs: false,
  setup() {
    return { ...useLoadingApi(), ...usePopups() };
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
    SvgThumbUpHollow,
    SvgThumbUpFilled,
    SvgThumbDownHollow,
    SvgThumbDownFilled,
  },
  emits: ["answered"],
  data() {
    return {
      answer: "" as string,
      markedAsGood: false,
      markedAsBad: false,
      markedGoodQuestionId: "" as string,
      markedBadQuestionId: "" as string,
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
    async markAsGood() {
      this.markedGoodQuestionId = await this.api.reviewMethods.markAsGood(
        this.quizQuestion.quizQuestionId,
        this.quizQuestion.notebookPosition?.noteId,
      );
      this.markedAsGood = true;
    },

    async unmarkAsGood() {
      await this.api.reviewMethods.unmarkAsGood(this.markedGoodQuestionId);
      this.markedGoodQuestionId = "";
      this.markedAsGood = false;
    },

    async markAsBad() {
      this.markedBadQuestionId = await this.api.reviewMethods.markAsBad(
        this.quizQuestion.quizQuestionId,
        this.quizQuestion.notebookPosition?.noteId,
      );
      this.markedAsBad = true;
    },

    async unmarkAsBad() {
      await this.api.reviewMethods.unmarkAsBad(this.markedBadQuestionId);
      this.markedBadQuestionId = "";
      this.markedAsBad = false;
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
  position: absolute;
  top: -0.3em;
  right: 0.7em;

  button {
    border: none;
    background: none;
  }
}
</style>
