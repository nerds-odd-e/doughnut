<template>
  <h2 v-if="rawJsonQuestion === undefined">Generating question...</h2>
  <div v-else>
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
    <button class="btn btn-secondary">Send</button>
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
  },
  methods: {
    async generateQuestion() {
      this.quizQuestion = await this.api.ai.askAIToGenerateQuestion(
        this.selectedNote.id
      );
      this.numberOfTries += 1;
    },
    async regenerateQuestion() {
      if (this.quizQuestion !== undefined) {
        this.quizQuestion = await this.api.ai.askAIToRegenerateQuestion(
          this.selectedNote.id,
          this.quizQuestion
        );
      }
      this.numberOfTries += 1;
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
</style>
