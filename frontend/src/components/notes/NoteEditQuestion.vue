<template>
  <div>
    <TextArea
      :rows="2"
      field="stem"
      v-model="formData.quizQuestion.multipleChoicesQuestion.stem"
    /><br />
  <div v-for="(_, index) in formData.quizQuestion.multipleChoicesQuestion.choices" :key="index">
    <TextArea
      :field="'choice ' + index"
      :rows="1"
      v-model="formData.quizQuestion.multipleChoicesQuestion.choices[index]"
    />
    <br />
  </div>

  <TextInput
    rows="2"
    field="correctChoiceIndex"
    v-model="formData.correctAnswerIndex"
  /><br />

  <button
    @click="addChoice"
    :disabled="
        formData.quizQuestion.multipleChoicesQuestion.choices.length >= maximumNumberOfChoices
      "
  >
    +
  </button>
  <button
    @click="removeChoice"
    :disabled="
        formData.quizQuestion.multipleChoicesQuestion.choices.length <= minimumNumberOfChoices
      "
  >
    -
  </button>
  <button @click="submitQuestion" >Submit</button>
  </div>
</template>

<script setup lang="ts">
import { computed, PropType, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
// import isMCQWithAnswerValid from "@/models/isMCQWithAnswerValid"
import { QuizQuestionAndAnswer } from "@/generated/backend"

const { managedApi } = useLoadingApi()
const props = defineProps({
  question: {
    type: Object as PropType<QuizQuestionAndAnswer>,
    required: true,
  },
})

const formData = ref<QuizQuestionAndAnswer>(
  props.question as QuizQuestionAndAnswer
)

const minimumNumberOfChoices = 2
const maximumNumberOfChoices = 10

const emit = defineEmits(["close-dialog"])

// const isValidQuestion = computed(() =>
//   isMCQWithAnswerValid(formData.value)
// )
const multipleChoicesQuestion = computed(
  () => formData.value.quizQuestion.multipleChoicesQuestion
)
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
  const response = await managedApi.restQuizQuestionController.editQuestion(
    props.question.id,
    formData.value
  )
  emit("close-dialog", response)
}
</script>
