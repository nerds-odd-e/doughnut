<template>
  <div class="quiz-instruction daisy-relative daisy-max-w-6xl daisy-mx-auto" data-test="question-section">
    <QuestionStem :stem="multipleChoicesQuestion.f0__stem" />
    <QuestionChoices
      v-if="multipleChoicesQuestion.f1__choices"
      :choices="multipleChoicesQuestion.f1__choices"
      :correct-choice-index="correctChoiceIndex"
      :answer-choice-index="answer?.choiceIndex"
      :disabled="disabled"
      @answer="submitAnswer($event)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onActivated, onDeactivated, watch } from "vue"
import type { PropType } from "vue"
import type {
  Answer,
  AnswerDto,
  MultipleChoicesQuestion,
} from "@generated/backend"
import QuestionChoices from "./QuestionChoices.vue"
import QuestionStem from "./QuestionStem.vue"
import { useThinkingTimeTracker } from "@/composables/useThinkingTimeTracker"

const props = defineProps({
  multipleChoicesQuestion: {
    type: Object as PropType<MultipleChoicesQuestion>,
    required: true,
  },
  correctChoiceIndex: Number,
  answer: Object as PropType<Answer>,
  disabled: Boolean,
})

const emits = defineEmits(["answer"])

const isActiveQuestion = computed(() => !props.disabled && !props.answer)

const { start, stop, pause, resume } = useThinkingTimeTracker()

watch(
  isActiveQuestion,
  (isActive) => {
    if (isActive) {
      start()
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (isActiveQuestion.value) {
    start()
  }
})

onActivated(() => {
  if (isActiveQuestion.value) {
    resume()
  }
})

onDeactivated(() => {
  pause()
})

const submitAnswer = async (answerData: AnswerDto) => {
  const thinkingTimeMs = stop()
  emits("answer", { ...answerData, thinkingTimeMs })
}
</script>
