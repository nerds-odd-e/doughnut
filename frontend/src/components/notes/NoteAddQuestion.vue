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
      v-model="mcq.correctAnswerIndex"
    /><br />
    <button
      @click="addChoice"
      :disabled="
        multipleChoicesQuestion.responseChoices.length >= maximumNumberOfChoices
      "
      class="daisy-btn daisy-btn-sm daisy-btn-outline mr-2"
    >
      +
    </button>
    <button
      @click="removeChoice"
      :disabled="
        multipleChoicesQuestion.responseChoices.length <= minimumNumberOfChoices
      "
      class="daisy-btn daisy-btn-sm daisy-btn-outline mr-2"
    >
      -
    </button>
    <button
      @click="refineQuestion"
      :disabled="!dirty"
      class="daisy-btn daisy-btn-sm daisy-btn-secondary mr-2"
    >
      Refine
    </button>
    <button
      @click="generateQuestionByAI"
      :disabled="dirty"
      class="daisy-btn daisy-btn-sm daisy-btn-accent mr-2"
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
import type { Note, Mcq } from "@generated/doughnut-backend-api"
import { McqController } from "@generated/doughnut-backend-api/sdk.gen"
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

const mcq = ref<Mcq>({
  correctAnswerIndex: 0,
  multipleChoicesQuestion: {
    questionStem: "",
    responseChoices: ["", ""],
  },
} as Mcq)

const minimumNumberOfChoices = 2
const maximumNumberOfChoices = 10

const emit = defineEmits(["close-dialog"])

const isValidQuestion = computed(() => isMCQWithAnswerValid(mcq.value))

const multipleChoicesQuestion = computed(
  () => mcq.value.multipleChoicesQuestion
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
  const { data: response, error } = await apiCallWithLoading(() =>
    McqController.addQuestionManually({
      path: { note: props.note.id },
      body: mcq.value,
    })
  )
  if (!error && response) {
    emit("close-dialog", response)
  }
}

const refineQuestion = async () => {
  const { data: refined, error } = await apiCallWithLoading(() =>
    McqController.refineQuestion({
      path: { note: props.note.id },
      body: mcq.value,
    })
  )
  if (!error && refined) {
    mcq.value = refined
  }
}

const generateQuestionByAI = async () => {
  const { data: generated, error } = await apiCallWithLoading(() =>
    McqController.generateQuestionWithoutSave({
      query: { note: props.note.id },
    })
  )
  if (!error && generated) {
    mcq.value = generated
  }
}
</script>
