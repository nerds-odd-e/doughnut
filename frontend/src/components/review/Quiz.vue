<template>
  <div v-if="!minimized" class="content">
    <div class="inner-box">
      <template v-if="currentQuizQuestion">
        <QuizQuestion
          v-bind="{
            quizQuestion: currentQuizQuestion,
            reviewPointId: currentReviewPointId,
            storageAccessor,
          }"
          @answered="onAnswered($event)"
          :key="currentQuizQuestion.quizQuestionId"
        />
      </template>
      <template
        v-else-if="
          quizQuestions !== undefined &&
          quizQuestions.length === currentQuestionIndex
        "
      >
        <div class="alert alert-success">
          You have finished all repetitions for this half a day!
        </div>
      </template>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import _ from "lodash";
import QuizQuestion from "./QuizQuestion.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    minimized: Boolean,
    quizQuestions: {
      type: Object as PropType<number[]>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["answered"],
  components: {
    QuizQuestion,
  },
  data() {
    return {
      currentQuestionIndex: 0,
      currentQuizQuestion: undefined as Generated.QuizQuestion | undefined,
    };
  },
  computed: {
    currentReviewPointId() {
      return this.quizQuestions[this.currentQuestionIndex] as number;
    },
  },
  watch: {
    minimized() {
      this.selectPosition();
    },
    toRepeat() {
      this.currentQuestionIndex = 0;
      this.fetchQuestion();
    },
  },
  methods: {
    selectPosition() {
      if (this.minimized) return;
      this.storageAccessor.selectPosition(
        undefined,
        this.currentQuizQuestion?.notebookPosition
      );
    },

    async fetchQuestion() {
      if (
        !this.quizQuestions ||
        this.quizQuestions.length === this.currentQuestionIndex
      ) {
        return;
      }
      this.currentQuizQuestion =
        await this.api.reviewMethods.getRandomQuestionForReviewPoint(
          this.currentReviewPointId
        );
      this.selectPosition();
    },

    onAnswered(answerResult: Generated.AnswerResult) {
      this.$emit("answered", answerResult);
      this.currentQuestionIndex += 1;
      this.currentQuizQuestion = undefined;
      this.fetchQuestion();
    },
  },
  async mounted() {
    this.fetchQuestion();
  },
});
</script>
