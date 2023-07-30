<template>
  <div v-if="!minimized" class="content">
    <div class="inner-box">
      <template v-if="currentQuizQuestion">
        <div v-if="currentQuizQuestion.questionType === 'JUST_REVIEW'">
          <JustReview
            v-bind="{
              reviewPointId: currentReviewPointId,
              storageAccessor,
            }"
            @reviewed="onAnswered($event)"
          />
        </div>
        <QuizQuestion
          v-else
          v-bind="{
            quizQuestion: currentQuizQuestion,
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
import JustReview from "./JustReview.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    minimized: Boolean,
    reviewPoints: {
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
    JustReview,
  },
  data() {
    return {
      quizQuestionCache: new Map<number, Generated.QuizQuestion | undefined>(),
      quizQuestionCache1: [] as (Generated.QuizQuestion | undefined)[],
      nextFetchingIndex: 0,
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
      this.nextFetchingIndex = 0;
      this.fetchQuestion();
    },
    currentIndex() {
      this.fetchQuestion();
    },
  },
  methods: {
    nextReviewPointId(offset: number): number | undefined {
      return this.reviewPointIdAt(this.currentIndex + offset);
    },

    reviewPointIdAt(index: number): number | undefined {
      if (this.reviewPoints && index < this.reviewPoints.length) {
        return this.reviewPoints[index] as number;
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
      if (!this.reviewPoints) {
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
      if (this.nextFetchingIndex < this.currentIndex + index) {
        return;
      }
      this.nextFetchingIndex += 1;
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

    onAnswered(answerResult: Generated.AnsweredQuestion) {
      this.$emit("answered", answerResult);
    },
  },
  async mounted() {
    this.fetchQuestion();
  },
});
</script>
