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
  <div v-show="quizQuestion !== undefined" class="askInputContainer">
    <TextInput id="askInputText" class="askInputText" v-model="askInput" />
    <button id="askBtn" class="floatBtn">ASK</button>
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
</style>
