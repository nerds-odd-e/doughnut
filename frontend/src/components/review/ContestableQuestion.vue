<template>
  <BasicBreadcrumb
    :ancestors="[quizQuestionInNotebook.notebook.headNote.noteTopic]"
  />
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
      :quiz-question="currentQuestion.quizQuestion"
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
import {
  AnsweredQuestion,
  QuizQuestion,
  QuizQuestionInNotebook,
} from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import QuizQuestionC from "./QuizQuestion.vue";
import AnsweredQuestionComponent from "./AnsweredQuestionComponent.vue";
import BasicBreadcrumb from "../commons/BasicBreadcrumb.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    quizQuestionInNotebook: {
      type: Object as PropType<QuizQuestionInNotebook>,
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
    BasicBreadcrumb,
    AnsweredQuestionComponent,
  },
  data() {
    return {
      regenerating: false,
      currentQuestionLegitMessage: undefined as string | undefined,
      currentQuestion: this.quizQuestionInNotebook,
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
      const contestResult =
        await this.managedApi.restQuizQuestionController.contest(
          this.currentQuestion.quizQuestion.id,
        );

      if (!contestResult.rejected) {
        this.regenerating = true;
        this.prevQuizQuestions.push({
          quizeQuestion: this.currentQuestion.quizQuestion,
          badQuestionReason: contestResult.reason,
        });
        this.currentQuestion = {
          notebook: this.currentQuestion.notebook,
          quizQuestion:
            await this.managedApi.restQuizQuestionController.regenerate(
              this.currentQuestion.quizQuestion.id,
            ),
        };
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
