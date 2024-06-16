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

<script setup lang="ts">
import { PropType, computed, ref } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { Note, QuizQuestionAndAnswer } from "@/generated/backend";
import isMCQWithAnswerValid from "@/models/isMCQWithAnswerValid";
import isRefineMCQWithAnswerValid from "@/models/isRefineMCQWithAnswerValid";
import TextArea from "../form/TextArea.vue";

const { managedApi } = useLoadingApi();
const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
});

const quizQuestionAndAnswer = ref<QuizQuestionAndAnswer>({
  correctAnswerIndex: 0,
  quizQuestion: {
    multipleChoicesQuestion: {
      stem: "",
      choices: ["", ""],
    },
  },
} as QuizQuestionAndAnswer);
const minOptions = 2; // Minimum number of options
const maxOptions = 10; // Maximum number of options

const emit = defineEmits(["close-dialog"]);

const isValidQuestion = computed(() =>
  isMCQWithAnswerValid(quizQuestionAndAnswer.value),
);
const isValidRefine = computed(() =>
  isRefineMCQWithAnswerValid(quizQuestionAndAnswer.value),
);
const multipleChoicesQuestion = computed(
  () => quizQuestionAndAnswer.value.quizQuestion.multipleChoicesQuestion,
);
const isUserDidInputChoices = computed(() => {
  for (let i = 0; i < multipleChoicesQuestion.value.choices.length; i += 1) {
    if (multipleChoicesQuestion.value.choices[i]) {
      return true;
    }
  }
  return false;
});
const isUserDidInputQuestion = computed(() => {
  return (
    multipleChoicesQuestion.value.stem &&
    multipleChoicesQuestion.value.stem.trim().length > 0
  );
});

const addOption = () => {
  if (multipleChoicesQuestion.value.choices.length < maxOptions) {
    multipleChoicesQuestion.value.choices.push("");
  }
};

const removeOption = () => {
  if (multipleChoicesQuestion.value.choices.length > minOptions) {
    multipleChoicesQuestion.value.choices.pop();
  }
};
const submitQuestion = async () => {
  const quizQuestion = quizQuestionAndAnswer.value;
  const response =
    await managedApi.restQuizQuestionController.addQuestionManually(
      props.note.id,
      quizQuestion,
    );
  emit("close-dialog", response);
};
const refineQuestion = async () => {
  const quizQuestion = quizQuestionAndAnswer.value;
  const response = await managedApi.restQuizQuestionController.refineQuestion(
    props.note.id,
    quizQuestion,
  );
  quizQuestionAndAnswer.value = response;
};
const generateQuestionByAI = async () => {
  quizQuestionAndAnswer.value =
    await managedApi.restQuizQuestionController.generateAiQuestionWithoutSave(
      props.note.id,
    );
};
</script>
