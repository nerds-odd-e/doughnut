<template>
  <p>Let's talk about this note.</p>
  <div v-if="quizQuestion">
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
    <div>
      <h2>Suggest This Question For AI Fine Tuning</h2>
      <p>
        <i
          >Sending this question for fine tuning the question generation model
          will make this note and question visible to admin. Are you sure?</i
        >
      </p>
      <div>
        <button
          class="positive-feedback-btn feedback-btn"
          :class="{ selected: isPositive }"
          @click="markQuestionAsPositive"
        >
          Positive
        </button>
        <button
          class="negative-feedback-btn feedback-btn"
          :class="{ selected: !isPositive }"
          @click="markQuestionAsNegative"
        >
          Negative
        </button>
      </div>
      <TextInput
        id="feedback-comment"
        field="comment"
        v-model="comment"
        placeholder="Add a comment about the question"
      />
      <div class="feedback-actions-container">
        <button
          class="suggest-fine-tuning-ok-btn btn btn-success"
          @click="suggestQuestionForFineTuning"
        >
          OK
        </button>
        <div
          class="suggestion-sent-successfully-message"
          v-if="suggestionSubmittedSuccessfully"
        >
          Feedback sent successfully!
        </div>
      </div>
    </div>
  </div>
  <div v-show="answered" class="chat-answer-container">
    <img src="/user-icon.svg" class="chat-answer-icon" />
    <div class="chat-answer-text">
      <p id="chat-answer">{{ assistantMessage }}</p>
    </div>
  </div>

  <div class="fixed-bottom chat-control">
    <button
      v-if="!quizQuestion"
      class="btn btn-secondary"
      @click="generateQuestion"
    >
      Test me
    </button>
    <button
      id="try-again"
      v-if="quizQuestion"
      class="btn btn-secondary"
      @click="generateQuestion"
    >
      Doesn't make sense?
    </button>
    <div class="chat-container">
      <form class="chat-input-container" @submit.prevent="generateChatAnswer">
        <input id="chat-input" class="chat-input-text" v-model="chatInput" />
        <input
          :disabled="isButtonDisabled"
          type="submit"
          value="Chat"
          id="chat-button"
          class="btn float-btn btn-secondary"
        />
      </form>
    </div>
    <p class="text-center">
      <i
        >Ask question to AI about this note. Each question ignores the chat
        history, unlike ChatGPT.</i
      >
    </p>
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
      chatInput: "",
      assistantMessage: "",
      answered: false,
      comment: "",
      isPositive: null as boolean | null,
      suggestionSubmittedSuccessfully: false,
    };
  },
  computed: {
    isButtonDisabled() {
      return this.chatInput === "";
    },
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
    async generateChatAnswer() {
      this.assistantMessage = await this.api.ai.chat(
        this.selectedNote.id,
        this.chatInput,
      );
      this.answered = true;
    },
    markQuestionAsPositive() {
      this.isPositive = true;
    },
    markQuestionAsNegative() {
      this.isPositive = false;
    },
    async suggestQuestionForFineTuning() {
      try {
        await this.api.reviewMethods.suggestQuestionForFineTuning(
          this.quizQuestion!.quizQuestionId,
          {
            isPositive: this.isPositive ?? false,
            comment: this.comment,
          },
        );
        this.suggestionSubmittedSuccessfully = true;
      } catch (err) {
        this.suggestionSubmittedSuccessfully = false;
      }
    },
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

.chat-input-container {
  width: 100%;
  display: flex;
  flex-direction: row;
  padding-top: 5px;
  padding-bottom: 5px;
}

.chat-input-text {
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

.chat-answer-container {
  display: flex;
  margin: 2% 0;
}

.chat-answer-icon {
  width: 6%;
  height: 6%;
}

.chat-answer-text {
  position: relative;
  display: inline-block;
  margin-left: 15px;
  padding: 7px 10px;
  width: 100%;
  border: solid 3px #555;
  box-sizing: border-box;
}

.chat-answer-text:before {
  content: "";
  position: absolute;
  top: 50%;
  left: -24px;
  margin-top: -12px;
  border: 12px solid transparent;
  border-right: 12px solid #fff;
  z-index: 2;
}

.chat-answer-text:after {
  content: "";
  position: absolute;
  top: 50%;
  left: -30px;
  margin-top: -14px;
  border: 14px solid transparent;
  border-right: 14px solid #555;
}

.chat-answer-text p {
  margin: 0;
  word-break: break-word;
}

.chat-control {
  width: calc(100% - 140px);
  margin-left: auto;
  margin-right: 40px;
}

.feedback-btn.selected {
  background-color: red;
  color: white;
}

.feedback-actions-container {
  display: flex;
}
</style>
