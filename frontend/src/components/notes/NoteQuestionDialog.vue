<template>
  <h2 v-if="rawJsonQuestion === undefined">Generating question...</h2>
  <div v-else>
    <div v-if="rawJsonPrevQuestion !== undefined" :class="'disabled-div'">
      <h3>Previous Question...</h3>
      <AIQuestion
        :raw-json-question="rawJsonPrevQuestion"
        :key="numberOfTries"
      />
    </div>
    <AIQuestion
      :raw-json-question="rawJsonQuestion"
      :key="numberOfTries"
      @is-question-answered="getIsQuestionAnswered"
    />
  </div>
  <button
    v-show="!isQuestionAnswered && rawJsonQuestion !== undefined"
    class="btn btn-secondary"
    @click="regenerateQuestion"
  >
    Doesn't make sense?
  </button>

  <div id="chatContainer" v-show="isQuestionAnswered">
    <MessageDisplayContainer v-bind:messages="messages" />
    <div class="chatInputContainer">
      <button class="btn btn-secondary floatBtn" @click="sendMessage">
        Send
      </button>
      <span>
        <input
          v-model="userInputValue"
          class="autoExtendableInput"
          type="text"
        />
      </span>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import type { StorageAccessor } from "@/store/createNoteStorage";
import { Message } from "@/store/DialogMessage";
import useLoadingApi from "../../managedApi/useLoadingApi";
import AIQuestion from "../review/AIQuestion.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    selectedNote: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: false,
    },
  },
  components: { AIQuestion },
  data() {
    return {
      quizQuestion: undefined as Generated.QuizQuestion | undefined,
      prevQuizQuestion: undefined as Generated.QuizQuestion | undefined,
      numberOfTries: 0,
      isUnmounted: false,
      userInputValue: "",
      messages: [] as Message[],
      isQuestionAnswered: false,
    };
  },
  computed: {
    rawJsonQuestion() {
      return this.quizQuestion?.rawJsonQuestion;
    },
    rawJsonPrevQuestion() {
      return this.prevQuizQuestion?.rawJsonQuestion;
    },
  },
  methods: {
    async generateQuestion() {
      this.quizQuestion = await this.api.ai.askAIToGenerateQuestion(
        this.selectedNote.id,
      );
      this.numberOfTries += 1;
    },
    async regenerateQuestion() {
      if (this.quizQuestion !== undefined) {
        const tmpQuestion: Generated.QuizQuestion = this.quizQuestion;
        this.quizQuestion = await this.api.ai.askAIToRegenerateQuestion(
          this.selectedNote.id,
          this.quizQuestion.rawJsonQuestion,
        );
        this.prevQuizQuestion = tmpQuestion;
      }
      this.numberOfTries += 1;
    },
    async sendMessage() {
      if (this.userInputValue !== "") {
        const msg: Message = { role: "User", content: this.userInputValue };
        this.messages.push(msg);
      }
    },
    getIsQuestionAnswered(value: boolean) {
      this.isQuestionAnswered = value;
    },
  },
  mounted() {
    this.generateQuestion();
  },
  unmounted() {
    this.isUnmounted = true;
  },
});
</script>

<style lang="scss" scoped>
.is-correct {
  font-weight: bold;
  background-color: #00ff00;
}

.is-wrong {
  font-weight: bold;
  background-color: #ff0000;
}

span {
  display: block;
  overflow: hidden;
  padding-right: 5px;
}

.chatInputContainer {
  width: 100%;
}

input.autoExtendableInput {
  width: 100%;
}

.floatBtn {
  float: right;
}
</style>
