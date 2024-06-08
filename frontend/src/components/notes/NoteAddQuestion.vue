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
    <button @click="submitQuestion" :disabled="isInvalidQuestion">
      Submit
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { Note, MCQWithAnswer } from "@/generated/backend";
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
    isInvalidQuestion() {
      if (
        this.mcqWithAnswer.multipleChoicesQuestion.stem?.trim().length === 0
      ) {
        return true;
      }
      return (
        this.mcqWithAnswer.multipleChoicesQuestion.choices.some(
          (option) => option.trim().length === 0,
        ) ||
        this.mcqWithAnswer.correctChoiceIndex < 0 ||
        this.mcqWithAnswer.correctChoiceIndex >=
          this.mcqWithAnswer.multipleChoicesQuestion.choices.length
      );
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
  },
});
</script>
