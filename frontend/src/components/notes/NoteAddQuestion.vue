<template>
  <div>
    <TextArea
      :rows="2"
      field="stem"
      v-model="multipleChoicesQuestion.stem"
    /><br />

    <div v-for="(_, index) in multipleChoicesQuestion.choices" :key="index">
      <TextArea
        :field="'choice ' + index"
        :rows="1"
        v-model="multipleChoicesQuestion.choices[index]"
      />
      <br />
    </div>

    <TextInput
      rows="2"
      field="correctChoiceIndex"
      v-model="quizQuestionAndAnswer.correctAnswerIndex"
    /><br />

    <button
      @click="addOption"
      :disabled="multipleChoicesQuestion.choices.length >= maxOptions"
    >
      +
    </button>
    <button
      @click="removeOption"
      :disabled="multipleChoicesQuestion.choices.length <= minOptions"
    >
      -
    </button>
    <button @click="refineQuestion" :disabled="!isValidRefine">Refine</button>
    <button @click="submitQuestion" :disabled="!isValidQuestion">Submit</button>
    <button
      @click="generateQuestionByAI"
      :disabled="isUserDidInputQuestion || isUserDidInputChoices"
    >
      Generate by AI
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { Note, QuizQuestionAndAnswer } from "@/generated/backend";
import isMCQWithAnswerValid from "@/models/isMCQWithAnswerValid";
import isRefineMCQWithAnswerValid from "@/models/isRefineMCQWithAnswerValid";
import TextArea from "../form/TextArea.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    note: {
      type: Object as PropType<Note>,
      required: true,
    },
  },
  data() {
    return {
      quizQuestionAndAnswer: <QuizQuestionAndAnswer>{
        correctAnswerIndex: 0,
        quizQuestion: {
          multipleChoicesQuestion: {
            stem: "",
            choices: ["", ""],
          },
        },
      },
      minOptions: 2, // Minimum number of options
      maxOptions: 10, // Maximum number of options
    };
  },
  emits: ["close-dialog"],
  computed: {
    isValidQuestion() {
      return isMCQWithAnswerValid(this.quizQuestionAndAnswer);
    },
    isValidRefine() {
      return isRefineMCQWithAnswerValid(this.quizQuestionAndAnswer);
    },
    multipleChoicesQuestion() {
      return this.quizQuestionAndAnswer.quizQuestion.multipleChoicesQuestion;
    },
    isUserDidInputQuestion() {
      return (
        this.multipleChoicesQuestion.stem &&
        this.multipleChoicesQuestion.stem.trim().length > 0
      );
    },

    isUserDidInputChoices() {
      for (let i = 0; i < this.multipleChoicesQuestion.choices.length; i += 1) {
        if (this.multipleChoicesQuestion.choices[i]) {
          return true;
        }
      }
      return false;
    },
  },
  methods: {
    addOption() {
      if (this.multipleChoicesQuestion.choices.length < this.maxOptions) {
        this.multipleChoicesQuestion.choices.push("");
      }
    },

    removeOption() {
      if (this.multipleChoicesQuestion.choices.length > this.minOptions) {
        this.multipleChoicesQuestion.choices.pop();
      }
    },
    async submitQuestion() {
      const quizQuestion = this.quizQuestionAndAnswer;
      const response =
        await this.managedApi.restQuizQuestionController.addQuestionManually(
          this.note.id,
          quizQuestion,
        );
      this.$emit("close-dialog", response);
    },
    async refineQuestion() {
      const quizQuestion = this.quizQuestionAndAnswer;
      const response =
        await this.managedApi.restQuizQuestionController.refineQuestion(
          this.note.id,
          quizQuestion,
        );
      this.quizQuestionAndAnswer = response;
    },
    async generateQuestionByAI() {
      this.quizQuestionAndAnswer =
        await this.managedApi.restQuizQuestionController.generateAiQuestionWithoutSave(
          this.note.id,
        );
    },
  },
});
</script>
