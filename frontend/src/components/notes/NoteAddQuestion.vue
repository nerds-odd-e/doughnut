<template>
  <div>
    <TextArea
      :rows="2"
      field="stem"
      v-model="multipleChoicesQuestion.questionStem"
    /><br />
    <div v-for="(_, index) in multipleChoicesQuestion.responseChoices" :key="index">
      <TextArea
        :field="'choice ' + index"
        :rows="1"
        v-model="multipleChoicesQuestion.responseChoices[index]"
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
        multipleChoicesQuestion.responseChoices.length >= maximumNumberOfChoices
      "
      class="daisy-btn daisy-btn-sm daisy-btn-outline daisy-mr-2"
    >
      +
    </button>
    <button
      @click="removeChoice"
      :disabled="
        multipleChoicesQuestion.responseChoices.length <= minimumNumberOfChoices
      "
      class="daisy-btn daisy-btn-sm daisy-btn-outline daisy-mr-2"
    >
      -
    </button>
    <button
      @click="refineQuestion"
      :disabled="!dirty"
      class="daisy-btn daisy-btn-sm daisy-btn-secondary daisy-mr-2"
    >
      Refine
    </button>
    <button
      @click="generateQuestionByAI"
      :disabled="dirty"
      class="daisy-btn daisy-btn-sm daisy-btn-accent daisy-mr-2"
    >
      Generate by AI
    </button>
    <button
      @click="submitQuestion"
      :disabled="!isValidQuestion"
      class="daisy-btn daisy-btn-sm daisy-btn-primary"
    >
      Submit
    </button>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, ref } from "vue"
import type { Note, PredefinedQuestion } from "@generated/doughnut-backend-api"
import { PredefinedQuestionController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import isMCQWithAnswerValid from "@/models/isMCQWithAnswerValid"
import TextArea from "../form/TextArea.vue"
import TextInput from "../form/TextInput.vue"

const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
})

const predefinedQuestion = ref<PredefinedQuestion>({
  correctAnswerIndex: 0,
  multipleChoicesQuestion: {
    questionStem: "",
    responseChoices: ["", ""],
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
  for (
    let i = 0;
    i < multipleChoicesQuestion.value.responseChoices.length;
    i += 1
  ) {
    if (multipleChoicesQuestion.value.responseChoices[i]) {
      return true
    }
  }
  return (
    multipleChoicesQuestion.value.questionStem !== undefined &&
    multipleChoicesQuestion.value.questionStem.trim().length > 0
  )
})

const addChoice = () => {
  if (
    multipleChoicesQuestion.value.responseChoices.length <
    maximumNumberOfChoices
  ) {
    multipleChoicesQuestion.value.responseChoices.push("")
  }
}

const removeChoice = () => {
  if (
    multipleChoicesQuestion.value.responseChoices.length >
    minimumNumberOfChoices
  ) {
    multipleChoicesQuestion.value.responseChoices.pop()
  }
}

const submitQuestion = async () => {
  const recallPrompt = predefinedQuestion.value
  const { data: response, error } = await apiCallWithLoading(() =>
    PredefinedQuestionController.addQuestionManually({
      path: { note: props.note.id },
      body: recallPrompt,
    })
  )
  if (!error && response) {
    emit("close-dialog", response)
  }
}

const refineQuestion = async () => {
  const recallPrompt = predefinedQuestion.value
  const { data: refined, error } = await apiCallWithLoading(() =>
    PredefinedQuestionController.refineQuestion({
      path: { note: props.note.id },
      body: recallPrompt,
    })
  )
  if (!error && refined) {
    predefinedQuestion.value = refined
  }
}

const generateQuestionByAI = async () => {
  const { data: generated, error } = await apiCallWithLoading(() =>
    PredefinedQuestionController.generateQuestionWithoutSave({
      query: { note: props.note.id },
    })
  )
  if (!error && generated) {
    predefinedQuestion.value = generated
  }
}
</script>
