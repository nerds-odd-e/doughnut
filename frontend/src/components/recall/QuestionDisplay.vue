<template>
  <div class="quiz-instruction daisy-relative daisy-max-w-6xl daisy-mx-auto" data-test="question-section">
    <QuestionStem :stem="multipleChoicesQuestion.f0__stem" />
    <div class="daisy-text-xs daisy-text-gray-500 daisy-mt-2">
      Thinking time: {{ displayTime }}
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
import { ref, onMounted, onUnmounted } from "vue"
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

const { start, stop, updateAccumulatedTime } = useThinkingTimeTracker()
const displayTime = ref("0.0s")
let animationFrameId: number | null = null

const updateDisplay = () => {
  const ms = updateAccumulatedTime()
  const seconds = (ms / 1000).toFixed(1)
  displayTime.value = `${seconds}s`
  animationFrameId = requestAnimationFrame(updateDisplay)
}

onMounted(() => {
  start()
  updateDisplay()
})

onUnmounted(() => {
  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId)
  }
})

const submitAnswer = async (answerData: AnswerDto) => {
  const thinkingTimeMs = stop()
  emits("answer", { ...answerData, thinkingTimeMs })
}
</script>
