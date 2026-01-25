<template>
  <div>
    <div class="daisy-mb-4">
      <h3 class="daisy-text-lg daisy-font-bold daisy-mb-2">Edit Question</h3>
      <TextArea
        :rows="2"
        field="stem"
        v-model="editingQuestion.multipleChoicesQuestion.f0__stem"
      /><br />
      <div
        v-for="(_, index) in editingQuestion.multipleChoicesQuestion.f1__choices"
        :key="index"
      >
        <TextArea
          :field="'choice ' + index"
          :rows="1"
          v-model="editingQuestion.multipleChoicesQuestion.f1__choices[index]"
        />
        <br />
      </div>
      <TextInput
        rows="2"
        field="correctChoiceIndex"
        v-model="editingQuestion.correctAnswerIndex"
      /><br />
      <button
        @click="addChoice"
        :disabled="
          editingQuestion.multipleChoicesQuestion.f1__choices.length >=
          maximumNumberOfChoices
        "
        class="daisy-btn daisy-btn-sm daisy-btn-outline daisy-mr-2"
      >
        +
      </button>
      <button
        @click="removeChoice"
        :disabled="
          editingQuestion.multipleChoicesQuestion.f1__choices.length <=
          minimumNumberOfChoices
        "
        class="daisy-btn daisy-btn-sm daisy-btn-outline daisy-mr-2"
      >
        -
      </button>
      <button
        @click="saveQuestion"
        :disabled="!isValidQuestion"
        class="daisy-btn daisy-btn-sm daisy-btn-primary daisy-mr-2"
      >
        Save
      </button>
      <button
        @click="$emit('close-dialog')"
        class="daisy-btn daisy-btn-sm daisy-btn-secondary"
      >
        Cancel
      </button>
    </div>
    <div class="daisy-divider daisy-my-2"></div>
    <PopButton btn-class="btn-primary">
      <template #button_face>
        Send this question for fine tuning the question generation model
      </template>
      <template #default="{ closer }">
        <SuggestQuestionForFineTuning
          :predefined-question="predefinedQuestion"
          @close-dialog="closer()"
        />
      </template>
    </PopButton>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, ref, watch } from "vue"
import type { PredefinedQuestion } from "@generated/backend"
import { PredefinedQuestionController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import isMCQWithAnswerValid from "@/models/isMCQWithAnswerValid"
import SuggestQuestionForFineTuning from "../ai/SuggestQuestionForFineTuning.vue"
import TextArea from "../form/TextArea.vue"
import TextInput from "../form/TextInput.vue"
import PopButton from "../commons/Popups/PopButton.vue"

const props = defineProps({
  predefinedQuestion: {
    type: Object as PropType<PredefinedQuestion>,
    required: true,
  },
})

const emit = defineEmits<{
  (e: "close-dialog"): void
  (e: "question-updated", question: PredefinedQuestion): void
}>()

const minimumNumberOfChoices = 2
const maximumNumberOfChoices = 10

const editingQuestion = ref<PredefinedQuestion>({
  ...props.predefinedQuestion,
  multipleChoicesQuestion: {
    ...props.predefinedQuestion.multipleChoicesQuestion,
    f1__choices: [
      ...(props.predefinedQuestion.multipleChoicesQuestion.f1__choices || []),
    ],
  },
} as PredefinedQuestion)

watch(
  () => props.predefinedQuestion,
  (newQuestion) => {
    editingQuestion.value = {
      ...newQuestion,
      multipleChoicesQuestion: {
        ...newQuestion.multipleChoicesQuestion,
        f1__choices: [...(newQuestion.multipleChoicesQuestion.f1__choices || [])],
      },
    } as PredefinedQuestion
  },
  { deep: true }
)

const isValidQuestion = computed(() =>
  isMCQWithAnswerValid(editingQuestion.value)
)

const addChoice = () => {
  if (
    editingQuestion.value.multipleChoicesQuestion.f1__choices.length <
    maximumNumberOfChoices
  ) {
    editingQuestion.value.multipleChoicesQuestion.f1__choices.push("")
  }
}

const removeChoice = () => {
  if (
    editingQuestion.value.multipleChoicesQuestion.f1__choices.length >
    minimumNumberOfChoices
  ) {
    editingQuestion.value.multipleChoicesQuestion.f1__choices.pop()
  }
}

const saveQuestion = async () => {
  if (!props.predefinedQuestion.id) {
    return
  }
  const { data: updated, error } = await apiCallWithLoading(() =>
    PredefinedQuestionController.updateQuestion({
      path: { predefinedQuestion: props.predefinedQuestion.id },
      body: editingQuestion.value,
    })
  )
  if (!error && updated) {
    emit("question-updated", updated)
    emit("close-dialog")
  }
}
</script>
