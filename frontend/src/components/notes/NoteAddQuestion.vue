<template>
  <div>
    <label for="question">Question:</label>
    <TextArea id="question" rows="2" v-model="question" /><br />

    <div v-for="(_, index) in options" :key="index">
      <div v-if="index == 0">
        <label :for="'option' + (index + 1)">Option 1 (Correct Answer)</label>
        <TextArea
          :id="'option' + (index + 1)"
          :rows="1"
          v-model="options[index]"
        />
      </div>
      <div v-else>
        <label :for="'option' + (index + 1)">Option {{ index + 1 }}</label>
        <TextArea
          :id="'option' + (index + 1)"
          :rows="1"
          v-model="options[index]"
        />
      </div>
      <br />
    </div>

    <button @click="addOption" :disabled="options.length >= maxOptions">
      +
    </button>
    <button @click="removeOption" :disabled="options.length <= minOptions">
      -
    </button>
    <button @click="submitQuestion" :disabled="isInvalidQuestion">
      Submit
    </button>
    <span v-if="showAlert">{{ addQuestionManuallyResultMsg }}</span>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import {
  ApiError,
  Note,
  QuizQuestionCreationParams,
} from "@/generated/backend";
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
      question: "", // Initialize the question data property
      options: ["", ""], // Initialize with two options
      minOptions: 2, // Minimum number of options
      maxOptions: 10, // Maximum number of options
      placeHolder: { type: "Correct Answer", default: "-" },
      addQuestionManuallyResultMsg: "",
      showAlert: false,
    };
  },
  computed: {
    isInvalidQuestion() {
      // Check if any question or option is null or empty
      if (this.question.trim().length === 0) {
        return true;
      }
      return this.options.some((option) => option.trim().length === 0);
    },
  },
  methods: {
    addOption() {
      if (this.options.length < this.maxOptions) {
        this.options.push("");
      }
    },

    removeOption() {
      if (this.options.length > this.minOptions) {
        this.options.pop();
      }
    },
    convertFormResponseToMultipleChoice(
      note: Note,
    ): QuizQuestionCreationParams {
      const quizQuestion: QuizQuestionCreationParams = {
        noteId: note.id,
        correctAnswerIndex: 0,
        multipleChoicesQuestion: {
          stem: this.question,
          choices: this.options,
        },
      };

      return quizQuestion;
    },
    async submitQuestion() {
      try {
        const quizQuestion = this.convertFormResponseToMultipleChoice(
          this.note,
        );
        const response =
          await this.managedApi.restQuizQuestionController.addQuestionManually(
            this.note.id,
            quizQuestion,
          );
        this.showAlert = !response;
        this.addQuestionManuallyResultMsg = "";
      } catch (error) {
        const errorInstance = error as ApiError;
        this.addQuestionManuallyResultMsg = errorInstance.body.message;
        this.showAlert = true;
      }
    },
  },
});
</script>
