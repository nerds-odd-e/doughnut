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
      class="daisy-btn daisy-btn-sm daisy-btn-outline daisy-mr-2"
    >
      +
    </button>
    <button
      @click="removeChoice"
      :disabled="
        multipleChoicesQuestion.choices.length <= minimumNumberOfChoices
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
import type { Note, PredefinedQuestion } from "@generated/backend"
import {
  addQuestionManually,
  refineQuestion as refineQuestionApi,
  generateQuestionWithoutSave,
} from "@generated/backend/sdk.gen"
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
  const { data: response, error } = await addQuestionManually({
    path: { note: props.note.id },
    body: recallPrompt,
  })
  if (!error && response) {
    emit("close-dialog", response)
  }
}

const refineQuestion = async () => {
  const recallPrompt = predefinedQuestion.value
  const { data: refined, error } = await refineQuestionApi({
    path: { note: props.note.id },
    body: recallPrompt,
  })
  if (!error && refined) {
    predefinedQuestion.value = refined
  }
}

const generateQuestionByAI = async () => {
  const { data: generated, error } = await generateQuestionWithoutSave({
    query: { note: props.note.id },
  })
  if (!error && generated) {
    predefinedQuestion.value = generated
  }
}
</script>
