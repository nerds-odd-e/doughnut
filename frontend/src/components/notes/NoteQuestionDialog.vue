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
  <div v-show="quizQuestion !== undefined" class="askContainer">
    <div class="askInputContainer">
      <input id="ask-input" class="askInputText" v-model="askInput" />
      <button id="ask-button" class="floatBtn">ASK</button>
    </div>
    <div class="askAnswerContainer">
      <img src="/user-icon.svg" class="askAnswerIcon" />
      <div class="askAnswerText">
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
      askAnswer: "I'm ChatGPT",
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

.askInputContainer {
  width: 100%;
  display: flex;
  flex-direction: row;
  padding-top: 5px;
  padding-bottom: 5px;
}

.askInputText {
  width: 100%;
  margin-right: 5px;
  flex-grow: 1;
}
input.autoExtendableInput {
  width: 100%;
}

.floatBtn {
  float: right;
}

.askAnswerContainer {
  display: flex;
  margin: 2% 0;
}

.askAnswerIcon {
  width: 6%;
  height: 6%;
}

.askAnswerText {
  position: relative;
  display: inline-block;
  margin-left: 15px;
  padding: 7px 10px;
  width: 100%;
  border: solid 3px #555;
  box-sizing: border-box;
}

.askAnswerText:before {
  content: "";
  position: absolute;
  top: 50%;
  left: -24px;
  margin-top: -12px;
  border: 12px solid transparent;
  border-right: 12px solid #fff;
  z-index: 2;
}

.askAnswerText:after {
  content: "";
  position: absolute;
  top: 50%;
  left: -30px;
  margin-top: -14px;
  border: 14px solid transparent;
  border-right: 14px solid #555;
}

.askAnswerText p {
  margin: 0;
  word-break: break-word;
}
</style>
