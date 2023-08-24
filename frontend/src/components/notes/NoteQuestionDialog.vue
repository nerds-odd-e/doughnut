<template>
  <h2 v-if="!quizQuestion">Generating question...</h2>
  <div v-else>
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
      :quiz-question="quizQuestion"
      @answered="onAnswered($event)"
    />
  </div>
  <button
    v-show="quizQuestion !== undefined"
    class="btn btn-secondary"
    @click="generateQuestion"
  >
    Doesn't make sense?
  </button>
  <div v-show="quizQuestion !== undefined" class="ask-container">
    <form class="ask-input-container" @submit.prevent="generateAskAnswer">
      <input id="ask-input" class="ask-input-text" v-model="askInput" />
      <input
        type="submit"
        value="ASK"
        id="ask-button"
        class="btn float-btn btn-secondary"
      />
    </form>
    <div v-show="answered" class="ask-answer-container">
      <img src="/user-icon.svg" class="ask-answer-icon" />
      <div class="ask-answer-text">
        <p id="ask-answer">{{ askAnswer }}</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import type { StorageAccessor } from "@/store/createNoteStorage";
import useLoadingApi from "../../managedApi/useLoadingApi";
import QuizQuestion from "../review/QuizQuestion.vue";
import AnsweredQuestion from "../review/AnsweredQuestion.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    selectedNote: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { QuizQuestion, AnsweredQuestion },
  data() {
    return {
      quizQuestion: undefined as Generated.QuizQuestion | undefined,
      answeredQuestion: undefined as Generated.AnsweredQuestion | undefined,
      prevQuizQuestion: undefined as Generated.QuizQuestion | undefined,
      askInput: "",
      askAnswer: "",
      answered: false,
    };
  },
  methods: {
    async generateQuestion() {
      const tmpQuestion: Generated.QuizQuestion | undefined = this.quizQuestion;
      this.quizQuestion = await this.api.ai.askAIToGenerateQuestion(
        this.selectedNote.id,
      );
      this.prevQuizQuestion = tmpQuestion;
    },
    onAnswered(answeredQuestion: Generated.AnsweredQuestion) {
      this.answeredQuestion = answeredQuestion;
    },
    async generateAskAnswer() {
      this.answered = true;
      this.askAnswer = await this.api.chat.playChat(this.askInput);
    },
  },
  mounted() {
    this.generateQuestion();
  },
});
</script>

<style lang="scss" scoped>
.is-correct {
  font-weight: bold;
  background-color: #00ff00;
}

.is-incorrect {
  font-weight: bold;
  background-color: #ff0000;
}

span {
  display: block;
  overflow: hidden;
  padding-right: 5px;
}

.ask-input-container {
  width: 100%;
  display: flex;
  flex-direction: row;
  padding-top: 5px;
  padding-bottom: 5px;
}

.ask-input-text {
  width: 100%;
  margin-right: 5px;
  flex-grow: 1;
}
input.auto-extendable-input {
  width: 100%;
}

.float-btn {
  float: right;
}

.ask-answer-container {
  display: flex;
  margin: 2% 0;
}

.ask-answer-icon {
  width: 6%;
  height: 6%;
}

.ask-answer-text {
  position: relative;
  display: inline-block;
  margin-left: 15px;
  padding: 7px 10px;
  width: 100%;
  border: solid 3px #555;
  box-sizing: border-box;
}

.ask-answer-text:before {
  content: "";
  position: absolute;
  top: 50%;
  left: -24px;
  margin-top: -12px;
  border: 12px solid transparent;
  border-right: 12px solid #fff;
  z-index: 2;
}

.ask-answer-text:after {
  content: "";
  position: absolute;
  top: 50%;
  left: -30px;
  margin-top: -14px;
  border: 14px solid transparent;
  border-right: 14px solid #555;
}

.ask-answer-text p {
  margin: 0;
  word-break: break-word;
}
</style>
