<template>
  <div>
    <TextArea
      rows="2"
      field="stem"
      v-model="mcqWithAnswer.multipleChoicesQuestion.stem"
    /><br />

    <div
      v-for="(_, index) in mcqWithAnswer.multipleChoicesQuestion.choices"
      :key="index"
    >
      <TextArea
        :field="'choice ' + index"
        :rows="1"
        v-model="mcqWithAnswer.multipleChoicesQuestion.choices[index]"
      />
      <br />
    </div>

    <TextInput
      rows="2"
      field="correctChoiceIndex"
      v-model="mcqWithAnswer.correctChoiceIndex"
    /><br />

    <button
      @click="addOption"
      :disabled="
        mcqWithAnswer.multipleChoicesQuestion.choices.length >= maxOptions
      "
    >
      +
    </button>
    <button
      @click="removeOption"
      :disabled="
        mcqWithAnswer.multipleChoicesQuestion.choices.length <= minOptions
      "
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
import { Note, MCQWithAnswer } from "@/generated/backend";
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
      mcqWithAnswer: <MCQWithAnswer>{
        correctChoiceIndex: 0,
        multipleChoicesQuestion: {
          stem: "",
          choices: ["", ""],
        },
      },
      minOptions: 2, // Minimum number of options
      maxOptions: 10, // Maximum number of options
    };
  },
  emits: ["close-dialog"],
  computed: {
    isValidQuestion() {
      return isMCQWithAnswerValid(this.mcqWithAnswer);
    },
    isValidRefine() {
      return isRefineMCQWithAnswerValid(this.mcqWithAnswer);
    },
    isUserDidInputQuestion() {
      return (
        this.mcqWithAnswer.multipleChoicesQuestion.stem &&
        this.mcqWithAnswer.multipleChoicesQuestion.stem.trim.length > 0
      );
    },
    isUserDidInputChoices() {
      for (
        let i = 0;
        i < this.mcqWithAnswer.multipleChoicesQuestion.choices.length;
        i += 1
      ) {
        if (this.mcqWithAnswer.multipleChoicesQuestion.choices[i]) {
          return true;
        }
      }
      return false;
    },
  },
  methods: {
    addOption() {
      if (
        this.mcqWithAnswer.multipleChoicesQuestion.choices.length <
        this.maxOptions
      ) {
        this.mcqWithAnswer.multipleChoicesQuestion.choices.push("");
      }
    },

    removeOption() {
      if (
        this.mcqWithAnswer.multipleChoicesQuestion.choices.length >
        this.minOptions
      ) {
        this.mcqWithAnswer.multipleChoicesQuestion.choices.pop();
      }
    },
    async submitQuestion() {
      const quizQuestion = this.mcqWithAnswer;
      const response =
        await this.managedApi.restQuizQuestionController.addQuestionManually(
          this.note.id,
          quizQuestion,
        );
      this.$emit("close-dialog", response);
    },
    async refineQuestion() {
      const quizQuestion = this.mcqWithAnswer;
      const response =
        await this.managedApi.restQuizQuestionController.refineQuestion(
          this.note.id,
          quizQuestion,
        );
      this.mcqWithAnswer = response;
    },
    async generateQuestionByAI() {
      const response =
        await this.managedApi.restQuizQuestionController.generateQuestionForAssessmentWithoutSave(
          this.note.id,
        );
      this.mcqWithAnswer.multipleChoicesQuestion.stem =
        response.multipleChoicesQuestion?.stem;
      const choices = response.multipleChoicesQuestion;
      if (choices) {
        for (let i = 0; i < choices.choices.length; i += 1) {
          this.mcqWithAnswer.multipleChoicesQuestion.choices[i] =
            response.multipleChoicesQuestion?.choices[i] || "";
        }
      }
      this.mcqWithAnswer.correctChoiceIndex = response.correctAnswerIndex || 0;
    },
  },
});
</script>
