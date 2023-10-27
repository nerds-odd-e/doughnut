<template>
  <div class="quiz-question" v-if="quizQuestion1">
    <div v-if="prevQuizQuestion">
      <h3>Previous Question...</h3>
      <QuizQuestion :quiz-question="prevQuizQuestion" :disabled="true" />
    </div>
    <AnsweredQuestion
      v-if="answeredQuestion"
      :answered-question="answeredQuestion"
      :storage-accessor="storageAccessor"
    />
    <QuizQuestion
      v-else
      :quiz-question="quizQuestion1"
      @answered="onAnswered($event)"
    />
  </div>

  <a
    role="button"
    title="Doesn't make sense?"
    id="try-again"
    v-if="quizQuestion1"
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
import scrollToElement from "../commons/scrollToElement";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    selectedNote: { type: Object as PropType<Generated.Note>, required: true },
    quizQuestion: {
      type: Object as PropType<Generated.QuizQuestion>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    QuizQuestion,
    AnsweredQuestion,
  },
  data() {
    return {
      quizQuestion1: this.quizQuestion,
      answeredQuestion: undefined as Generated.AnsweredQuestion | undefined,
      prevQuizQuestion: undefined as Generated.QuizQuestion | undefined,
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
      const elm = this.$refs.bottomOfTheChat as HTMLElement;
      if (elm) {
        scrollToElement(elm);
      }
    },
    async generateQuestion() {
      const tmpQuestion: Generated.QuizQuestion | undefined =
        this.quizQuestion1;
      this.quizQuestion1 = await this.api.quizQuestions.contest(
        this.quizQuestion.quizQuestionId,
      );
      this.prevQuizQuestion = tmpQuestion;
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
