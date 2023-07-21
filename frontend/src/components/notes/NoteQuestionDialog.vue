<template>
  <h2 v-if="quizQuestion === undefined">Generating question...</h2>
  <div v-else>
    <div v-if="prevQuizQuestion">
      <h3>Previous Question...</h3>
      <QuizQuestion
        :quiz-question="prevQuizQuestion"
        :storage-accessor="storageAccessor"
        :disabled="true"
      />
    </div>
    <QuizQuestion
      :quiz-question="quizQuestion"
      :storage-accessor="storageAccessor"
    />
  </div>
  <button
    v-show="quizQuestion !== undefined"
    class="btn btn-secondary"
    @click="generateQuestion"
  >
    Doesn't make sense?
  </button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import type { StorageAccessor } from "@/store/createNoteStorage";
import useLoadingApi from "../../managedApi/useLoadingApi";
import QuizQuestion from "../review/QuizQuestion.vue";

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
  components: { QuizQuestion },
  data() {
    return {
      quizQuestion: undefined as Generated.QuizQuestion | undefined,
      prevQuizQuestion: undefined as Generated.QuizQuestion | undefined,
      answerResult: undefined as Generated.AnswerResult | undefined,
    };
  },
  methods: {
    async generateQuestion() {
      const tmpQuestion: Generated.QuizQuestion | undefined = this.quizQuestion;
      this.quizQuestion = await this.api.ai.askAIToGenerateQuestion(
        this.selectedNote.id,
        this.quizQuestion?.rawJsonQuestion,
      );
      this.prevQuizQuestion = tmpQuestion;
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
