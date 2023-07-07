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
    <AIQuestion :raw-json-question="rawJsonQuestion" :key="numberOfTries" />
  </div>
  <button
    id="generateBtn"
    class="btn btn-secondary"
    @click="regenerateQuestion"
  >
    Doesn't make sense?
  </button>

  <div id="chatContainer">
    <MessageDisplayContainer v-bind:messages="messages" />
    <input v-model="userInputValue" type="text" />
    <button class="btn btn-secondary" @click="sendMessage">Send</button>
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
        this.selectedNote.id
      );
      this.numberOfTries += 1;
    },
    async regenerateQuestion() {
      this.prevQuizQuestion = this.quizQuestion;
      if (this.quizQuestion !== undefined) {
        this.quizQuestion = await this.api.ai.askAIToRegenerateQuestion(
          this.selectedNote.id,
          this.quizQuestion.rawJsonQuestion
        );
      }
      this.numberOfTries += 1;
    },
    async sendMessage() {
      if (this.userInputValue !== "") {
        const msg: Message = { role: "User", content: this.userInputValue };
        this.messages.push(msg);
      }
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

.disabled-div {
  pointer-events: none;
  color: #aaa7a7;
}
</style>
