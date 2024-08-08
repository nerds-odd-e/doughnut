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
      @click="addChoice"
      :disabled="
        multipleChoicesQuestion.choices.length >= maximumNumberOfChoices
      "
    >
      +
    </button>
    <button
      @click="removeChoice"
      :disabled="
        multipleChoicesQuestion.choices.length <= minimumNumberOfChoices
      "
    >
      -
    </button>
    <button @click="refineQuestion" :disabled="!dirty">Refine</button>
    <button @click="generateQuestionByAI" :disabled="dirty">
      Generate by AI
    </button>
    <button @click="submitQuestion" :disabled="!isValidQuestion">Submit</button>
  </div>
</template>

<script setup lang="ts">
import { PropType, computed, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { Note, QuizQuestionAndAnswer } from "@/generated/backend"
import isMCQWithAnswerValid from "@/models/isMCQWithAnswerValid"
import TextArea from "../form/TextArea.vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
  question: {
    type: Object as PropType<QuizQuestionAndAnswer>,
    required: false,
  },
})

const quizQuestionAndAnswer = ref<QuizQuestionAndAnswer>(
  props.question
    ? {
        ...props.question,
        quizQuestion: {
          ...props.question.quizQuestion,
          multipleChoicesQuestion: {
            ...props.question.quizQuestion.multipleChoicesQuestion,
            choices: [
              ...props.question.quizQuestion.multipleChoicesQuestion.choices,
            ],
          },
        },
      }
    : ({
        correctAnswerIndex: 0,
        quizQuestion: {
          multipleChoicesQuestion: {
            stem: "",
            choices: ["", ""],
          },
        },
      } as QuizQuestionAndAnswer)
)

const minimumNumberOfChoices = 2
const maximumNumberOfChoices = 10

const emit = defineEmits(["close-dialog"])

const isValidQuestion = computed(() =>
  isMCQWithAnswerValid(quizQuestionAndAnswer.value)
)
const multipleChoicesQuestion = computed(
  () => quizQuestionAndAnswer.value.quizQuestion.multipleChoicesQuestion
)
const dirty = computed(() => {
  for (let i = 0; i < multipleChoicesQuestion.value.choices.length; i += 1) {
    if (multipleChoicesQuestion.value.choices[i]) {
      return true
    }
  }
  return (
    multipleChoicesQuestion.value.stem !== undefined &&
    multipleChoicesQuestion.value.stem.trim().length > 0
  )
})

const addChoice = () => {
  if (multipleChoicesQuestion.value.choices.length < maximumNumberOfChoices) {
    multipleChoicesQuestion.value.choices.push("")
  }
}

const removeChoice = () => {
  if (multipleChoicesQuestion.value.choices.length > minimumNumberOfChoices) {
    multipleChoicesQuestion.value.choices.pop()
  }
}
const submitQuestion = async () => {
  const quizQuestion = quizQuestionAndAnswer.value
  const response = props.question
    ? await managedApi.restQuizQuestionController.editQuestion(
        props.note.id,
        quizQuestion.id,
        quizQuestion
      )
    : await managedApi.restQuizQuestionController.addQuestionManually(
        props.note.id,
        quizQuestion
      )
  emit("close-dialog", response)
}
const refineQuestion = async () => {
  const quizQuestion = quizQuestionAndAnswer.value
  quizQuestionAndAnswer.value =
    await managedApi.restQuizQuestionController.refineQuestion(
      props.note.id,
      quizQuestion
    )
}
const generateQuestionByAI = async () => {
  quizQuestionAndAnswer.value =
    await managedApi.restQuizQuestionController.generateAiQuestionWithoutSave(
      props.note.id
    )
}
</script>
