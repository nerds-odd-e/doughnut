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
    <button @click="updateQuestion" :disabled="!isValidQuestion">Update</button>
    <button @click="cancelEdit">Cancel</button>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, ref, onMounted } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { Note, PredefinedQuestion } from "@/generated/backend"
import isMCQWithAnswerValid from "@/models/isMCQWithAnswerValid"
import TextArea from "../form/TextArea.vue"
import TextInput from "../form/TextInput.vue"
import { useToast } from "vue-toastification"

const toast = useToast()
const { managedApi } = useLoadingApi()

const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
  question: {
    type: Object as PropType<PredefinedQuestion>,
    required: true,
  }
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

const emit = defineEmits(["close-dialog", "question-updated"])

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

const updateQuestion = async () => {
  try {
    if (predefinedQuestion.value && predefinedQuestion.value.id) {
    //   const updatedQuestion = await managedApi.restPredefinedQuestionController.updateQuestion(
    //     predefinedQuestion.value.id,
    //     predefinedQuestion.value
    //   )
    //   emit("question-updated", updatedQuestion)
    //   toast.success("Question successfully updated")
    }
  } catch (error) {
    toast.error("An error occurred while updating the question")
    console.error("Question update error:", error)
  }
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

const cancelEdit = () => {
  emit("close-dialog")
}

onMounted(() => {
  // Clone the question to avoid modifying the original directly
  predefinedQuestion.value = JSON.parse(JSON.stringify(props.question))
})
</script>
