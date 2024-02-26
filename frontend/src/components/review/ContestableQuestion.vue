<template>
  <div v-for="(q, index) in prevQuizQuestions" :key="index">
    <h3>Previous Question Contested ...</h3>
    <p>{{ q.badQuestionReason }}</p>
    <QuizQuestionC :quiz-question="q.quizeQuestion" :disabled="true" />
  </div>
  <p v-if="currentQuestionLegitMessage">{{ currentQuestionLegitMessage }}</p>
  <ContentLoader v-if="regenerating" />
  <div class="quiz-question" v-else>
    <AnsweredQuestionComponent
      v-if="answeredQuestion"
      :answered-question="answeredQuestion"
      :storage-accessor="storageAccessor"
    />
    <QuizQuestionC
      v-else
      :quiz-question="currentQuestion"
      @answered="onAnswered($event)"
    >
      <a
        role="button"
        title="Doesn't make sense?"
        id="try-again"
        v-if="currentQuestion"
        class="btn"
        @click="contest"
      >
        <SvgContest />
      </a>
    </QuizQuestionC>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import type { StorageAccessor } from "@/store/createNoteStorage";
import { AnsweredQuestion, QuizQuestion } from "@/generated/backend";
import useLoadingApi from "../../managedApi/useLoadingApi";
import QuizQuestionC from "./QuizQuestion.vue";
import AnsweredQuestionComponent from "./AnsweredQuestionComponent.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    quizQuestion: {
      type: Object as PropType<QuizQuestion>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["need-scroll", "answered"],
  components: {
    QuizQuestionC,
    AnsweredQuestionComponent,
  },
  data() {
    return {
      regenerating: false,
      currentQuestionLegitMessage: undefined as string | undefined,
      currentQuestion: this.quizQuestion,
      answeredQuestion: undefined as AnsweredQuestion | undefined,
      prevQuizQuestions: [] as {
        quizeQuestion: QuizQuestion;
        badQuestionReason: string | undefined;
      }[],
      chatInput: "",
      assistantMessage: "",
      answered: false,
    };
  },
  computed: {
    isButtonDisabled() {
      return this.chatInput === "";
    },
  },
  methods: {
    scrollToBottom() {
      this.$emit("need-scroll");
    },
    async contest() {
      this.currentQuestionLegitMessage = "";
      const contestResult = await this.api.quizQuestions.contest(
        this.currentQuestion.id,
      );

      if (!contestResult.rejected) {
        this.regenerating = true;
        this.prevQuizQuestions.push({
          quizeQuestion: this.currentQuestion,
          badQuestionReason: contestResult.reason,
        });
        this.currentQuestion = await this.api.quizQuestions.regenerateQuestion(
          this.currentQuestion.id,
        );
      } else {
        this.currentQuestionLegitMessage = contestResult.reason;
      }
      this.regenerating = false;
      this.scrollToBottom();
    },
    onAnswered(answeredQuestion: AnsweredQuestion) {
      this.answeredQuestion = answeredQuestion;
      this.$emit("answered", answeredQuestion);
    },
  },
});
</script>

<style lang="scss" scoped>
.quiz-question {
  overflow-y: auto;
}
</style>
