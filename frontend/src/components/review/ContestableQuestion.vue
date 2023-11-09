<template>
  <div v-for="(q, index) in prevQuizQuestions" :key="index">
    <h3>Previous Question Contested ...</h3>
    <p>{{ q.badQuestionReason }}</p>
    <QuizQuestion :quiz-question="q.quizeQuestion" :disabled="true" />
  </div>
  <p v-if="currentQuestionLegitMessage">{{ currentQuestionLegitMessage }}</p>
  <div class="quiz-question" v-if="currentQuestion">
    <AnsweredQuestion
      v-if="answeredQuestion"
      :answered-question="answeredQuestion"
      :storage-accessor="storageAccessor"
    />
    <QuizQuestion
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
    </QuizQuestion>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import type { StorageAccessor } from "@/store/createNoteStorage";
import useLoadingApi from "../../managedApi/useLoadingApi";
import QuizQuestion from "./QuizQuestion.vue";
import AnsweredQuestion from "./AnsweredQuestion.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    quizQuestion: {
      type: Object as PropType<Generated.QuizQuestion>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["need-scroll", "answered"],
  components: {
    QuizQuestion,
    AnsweredQuestion,
  },
  data() {
    return {
      currentQuestionLegitMessage: "",
      currentQuestion: this.quizQuestion,
      answeredQuestion: undefined as Generated.AnsweredQuestion | undefined,
      prevQuizQuestions: [] as {
        quizeQuestion: Generated.QuizQuestion;
        badQuestionReason: string;
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
      const tmpQuestion: Generated.QuizQuestion | undefined =
        this.currentQuestion;
      const contestResult = await this.api.quizQuestions.contest(
        this.currentQuestion.quizQuestionId,
      );

      if (contestResult.newQuizQuestion) {
        this.currentQuestion = contestResult.newQuizQuestion;
        this.prevQuizQuestions.push({
          quizeQuestion: tmpQuestion,
          badQuestionReason: contestResult.reason,
        });
      } else {
        this.currentQuestionLegitMessage = contestResult.reason;
      }
      this.scrollToBottom();
    },
    onAnswered(answeredQuestion: Generated.AnsweredQuestion) {
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
