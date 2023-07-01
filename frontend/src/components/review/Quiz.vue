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
    currentIndex: {
      type: Number,
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
      currentQuizQuestion: undefined as Generated.QuizQuestion | undefined,
    };
  },
  computed: {
    currentReviewPointId() {
      return this.quizQuestions[this.currentIndex] as number;
    },
  },
  watch: {
    minimized() {
      this.selectPosition();
    },
    QuizQuestions() {
      this.fetchQuestion();
    },
    currentIndex() {
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
      this.currentQuizQuestion = undefined;
      if (!this.quizQuestions) {
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
    },
  },
  async mounted() {
    this.fetchQuestion();
  },
});
</script>
