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
    eagerFetchCount: {
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
      quizQuestionCache: new Map<number, Generated.QuizQuestion | undefined>(),
    };
  },
  computed: {
    currentReviewPointId() {
      return this.nextReviewPointId(0) as number;
    },
    currentQuizQuestion() {
      return this.quizQuestionCache.get(this.currentReviewPointId);
    },
  },
  watch: {
    minimized() {
      this.selectPosition();
    },
    QuizQuestions() {
      this.quizQuestionCache.clear();
      this.fetchQuestion();
    },
    currentIndex() {
      this.fetchQuestion();
    },
  },
  methods: {
    nextReviewPointId(index: number): number | undefined {
      if (
        this.quizQuestions &&
        index + this.currentIndex < this.quizQuestions.length
      ) {
        return this.quizQuestions[this.currentIndex + index] as number;
      }
      return undefined;
    },
    selectPosition() {
      if (this.minimized) return;
      this.storageAccessor.selectPosition(
        undefined,
        this.currentQuizQuestion?.notebookPosition,
      );
    },

    async fetchQuestion() {
      if (!this.quizQuestions) {
        return;
      }
      await this.fetchNextQuestion(0);
      this.selectPosition();
      await this.eagerFetch(1);
    },

    async eagerFetch(index: number) {
      if (index < this.eagerFetchCount) {
        await this.fetchNextQuestion(index);
        await this.eagerFetch(index + 1);
      }
    },

    async fetchNextQuestion(index: number) {
      const next = this.nextReviewPointId(index);
      if (next) {
        if (this.quizQuestionCache.has(next)) {
          return;
        }
        this.quizQuestionCache.set(next, undefined);
        this.quizQuestionCache.set(
          next,
          await this.silentApi.reviewMethods.getRandomQuestionForReviewPoint(
            next,
          ),
        );
      }
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
