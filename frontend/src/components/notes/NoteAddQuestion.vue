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
      v-model="predefinedQuestion.correctAnswerIndex"
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
import type { PropType } from "vue"
import { computed, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { Note, PredefinedQuestion } from "@/generated/backend"
import isMCQWithAnswerValid from "@/models/isMCQWithAnswerValid"
import TextArea from "../form/TextArea.vue"
import TextInput from "../form/TextInput.vue"

const { managedApi } = useLoadingApi()

const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
})

const predefinedQuestion = ref<PredefinedQuestion>({
  correctAnswerIndex: 0,
  multipleChoicesQuestion: {
    stem: "",
    choices: ["", ""],
  },
} as PredefinedQuestion)

const minimumNumberOfChoices = 2
const maximumNumberOfChoices = 10

const emit = defineEmits(["close-dialog"])

const isValidQuestion = computed(() =>
  isMCQWithAnswerValid(predefinedQuestion.value)
)

const multipleChoicesQuestion = computed(
  () => predefinedQuestion.value.multipleChoicesQuestion
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
  const recallPrompt = predefinedQuestion.value
  const response =
    await managedApi.restPredefinedQuestionController.addQuestionManually(
      props.note.id,
      recallPrompt
    )
  emit("close-dialog", response)
}

const refineQuestion = async () => {
  const recallPrompt = predefinedQuestion.value
  predefinedQuestion.value =
    await managedApi.restPredefinedQuestionController.refineQuestion(
      props.note.id,
      recallPrompt
    )
}

const generateQuestionByAI = async () => {
  predefinedQuestion.value =
    await managedApi.restPredefinedQuestionController.generateQuestionWithoutSave(
      props.note.id
    )
}
</script>
