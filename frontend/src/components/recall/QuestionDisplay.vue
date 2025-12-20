<template>
  <div class="quiz-instruction daisy-relative daisy-max-w-6xl daisy-mx-auto" data-test="question-section">
    <QuestionStem :stem="multipleChoicesQuestion.f0__stem" />
    <div v-if="isActiveQuestion" class="daisy-text-xs daisy-text-gray-500 daisy-mt-2">
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
import {
  ref,
  computed,
  onMounted,
  onUnmounted,
  onActivated,
  onDeactivated,
  watch,
} from "vue"
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

const { start, stop, pause, resume, updateAccumulatedTime } =
  useThinkingTimeTracker()
const displayTime = ref("0.0s")
let animationFrameId: number | null = null

const updateDisplay = () => {
  if (!isActiveQuestion.value) {
    return
  }
  const ms = updateAccumulatedTime()
  const seconds = (ms / 1000).toFixed(1)
  displayTime.value = `${seconds}s`
  animationFrameId = requestAnimationFrame(updateDisplay)
}

watch(
  isActiveQuestion,
  (isActive) => {
    if (isActive) {
      start()
      updateDisplay()
    } else if (animationFrameId !== null) {
      cancelAnimationFrame(animationFrameId)
      animationFrameId = null
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (isActiveQuestion.value) {
    start()
    updateDisplay()
  }
})

onActivated(() => {
  if (isActiveQuestion.value) {
    resume()
    updateDisplay()
  }
})

onDeactivated(() => {
  pause()
  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
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
