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
    <button @click="submitQuestion" :disabled="!isValidQuestion">Submit</button>
  </div>
</template>

<script setup lang="ts">
import { PropType, computed, ref, onMounted } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { QuizQuestionAndAnswer } from "@/generated/backend"
import isMCQWithAnswerValid from "@/models/isMCQWithAnswerValid"
import TextArea from "../form/TextArea.vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  quizQuestion: {
    type: Object as PropType<QuizQuestionAndAnswer>,
    required: true,
  },
})

const quizQuestionAndAnswer = ref<QuizQuestionAndAnswer>({
  correctAnswerIndex: 0,
  quizQuestion: {
    multipleChoicesQuestion: {
      stem: "",
      choices: ["", ""],
    },
  },
} as QuizQuestionAndAnswer)

const minimumNumberOfChoices = 2
const maximumNumberOfChoices = 10

const emit = defineEmits(["close-dialog"])

const isValidQuestion = computed(() =>
  isMCQWithAnswerValid(quizQuestionAndAnswer.value)
)
const multipleChoicesQuestion = computed(
  () => quizQuestionAndAnswer.value.quizQuestion.multipleChoicesQuestion
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
  const quizQuestion = quizQuestionAndAnswer.value
  const response = await managedApi.restQuizQuestionController.editQuestion(
    props.quizQuestion.id,
    quizQuestion
  )
  emit("close-dialog", response)
}

const patchValue = () => {
  quizQuestionAndAnswer.value = props.quizQuestion
}

onMounted(() => {
  patchValue()
})
</script>
