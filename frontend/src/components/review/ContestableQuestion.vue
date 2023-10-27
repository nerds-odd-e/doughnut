<template>
  <div class="quiz-question" v-if="currentQuestion">
    <div v-for="(q, index) in prevQuizQuestions" :key="index">
      <h3>Previous Question...</h3>
      <QuizQuestion :quiz-question="q" :disabled="true" />
    </div>
    <AnsweredQuestion
      v-if="answeredQuestion"
      :answered-question="answeredQuestion"
      :storage-accessor="storageAccessor"
    />
    <QuizQuestion
      v-else
      :quiz-question="currentQuestion"
      @answered="onAnswered($event)"
    />
  </div>

  <a
    role="button"
    title="Doesn't make sense?"
    id="try-again"
    v-if="currentQuestion"
    class="btn btn-secondary"
    @click="generateQuestion"
  >
    <SvgContest />
  </a>
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
  emits: ["need-scroll"],
  components: {
    QuizQuestion,
    AnsweredQuestion,
  },
  data() {
    return {
      currentQuestion: this.quizQuestion,
      answeredQuestion: undefined as Generated.AnsweredQuestion | undefined,
      prevQuizQuestions: [] as Generated.QuizQuestion[],
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
    async generateQuestion() {
      const tmpQuestion: Generated.QuizQuestion | undefined =
        this.currentQuestion;
      this.currentQuestion = await this.api.quizQuestions.contest(
        this.quizQuestion.quizQuestionId,
      );
      this.prevQuizQuestions.push(tmpQuestion);
      this.scrollToBottom();
    },
    onAnswered(answeredQuestion: Generated.AnsweredQuestion) {
      this.answeredQuestion = answeredQuestion;
    },
  },
});
</script>

<style lang="scss" scoped>
.quiz-question {
  overflow-y: auto;
}
</style>
