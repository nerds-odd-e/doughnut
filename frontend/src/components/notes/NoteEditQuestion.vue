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
    <button @click="updateQuestion" :disabled="!isValidQuestion">Update</button>
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
    required: true,
  },
})

const quizQuestionAndAnswer = ref<QuizQuestionAndAnswer>({
  id: 0,
  correctAnswerIndex: 0,
  quizQuestion: {
    ...props.question.quizQuestion,
    multipleChoicesQuestion: {
      stem: props.question.quizQuestion?.multipleChoicesQuestion?.stem || "",
      choices: props.question.quizQuestion?.multipleChoicesQuestion.choices || [
        "",
        "",
      ],
    },
  },
})

const minimumNumberOfChoices = 2
const maximumNumberOfChoices = 10

const emit = defineEmits(["close-dialog"])

const isValidQuestion = computed(() =>
  isMCQWithAnswerValid(quizQuestionAndAnswer.value)
)
const multipleChoicesQuestion = computed(
  () => quizQuestionAndAnswer.value.quizQuestion.multipleChoicesQuestion
)
// const dirty = computed(() => {
//   for (let i = 0; i < multipleChoicesQuestion.value.choices.length; i += 1) {
//     if (multipleChoicesQuestion.value.choices[i]) {
//       return true
//     }
//   }
//   return (
//     multipleChoicesQuestion.value.stem !== undefined &&
//     multipleChoicesQuestion.value.stem.trim().length > 0
//   )
// })

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
const updateQuestion = async () => {
  const quizQuestion = quizQuestionAndAnswer.value
  console.log(quizQuestion)
  await managedApi.restQuizQuestionController.updateQuestionManually(
    props.note.id,
    quizQuestion
  )
  // emit("close-dialog", response)
}
</script>
