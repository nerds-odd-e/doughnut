<template>
  <div class="quiz-instruction daisy-relative daisy-max-w-6xl daisy-mx-auto" data-test="question-section">
    <QuestionStem :stem="multipleChoicesQuestion.f0__stem" />
    <div v-if="!disabled && currentTimeMs > 0" class="daisy-text-sm daisy-text-base-content/70 daisy-mt-2">
      Thinking time: {{ formatThinkingTime(currentTimeMs) }}
    </div>
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
import { onMounted } from "vue"
import type { PropType } from "vue"
import type {
  Answer,
  AnswerDto,
  MultipleChoicesQuestion,
} from "@generated/backend"
import QuestionChoices from "./QuestionChoices.vue"
import QuestionStem from "./QuestionStem.vue"
import { useThinkingTimeTracker } from "@/composables/useThinkingTimeTracker"

defineProps({
  multipleChoicesQuestion: {
    type: Object as PropType<MultipleChoicesQuestion>,
    required: true,
  },
  correctChoiceIndex: Number,
  answer: Object as PropType<Answer>,
  disabled: Boolean,
})

const emits = defineEmits(["answer"])

const { start, stop, currentTimeMs } = useThinkingTimeTracker()

const formatThinkingTime = (ms: number): string => {
  if (ms < 1000) {
    return `${ms}ms`
  }
  const seconds = ms / 1000
  if (seconds < 60) {
    return `${seconds.toFixed(1)}s`
  }
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = Math.floor(seconds % 60)
  return `${minutes}m ${remainingSeconds}s`
}

onMounted(() => {
  start()
})

const submitAnswer = async (answerData: AnswerDto) => {
  const thinkingTimeMs = stop()
  emits("answer", { ...answerData, thinkingTimeMs })
}
</script>
