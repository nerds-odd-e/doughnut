<template>
  <div class="quiz-instruction daisy-relative daisy-h-full daisy-flex daisy-flex-col" data-test="question-section">
    <ContentLoader v-if="loading" />
    <template v-else>
      <div class="daisy-flex-1 daisy-overflow-y-auto daisy-pb-4">
        <div v-if="recallPrompt?.spellingQuestion?.notebook" class="notebook-source daisy-mb-4">
          <NotebookLink :notebook="recallPrompt.spellingQuestion.notebook" />
        </div>
        <QuestionStem :stem="stem" />
        <div class="daisy-text-xs daisy-text-gray-500 daisy-mt-2">
          Thinking time: {{ displayTime }}
        </div>
      </div>
      <form @submit.prevent="submitAnswer" class="daisy-sticky daisy-bottom-0 daisy-bg-base-100 daisy-pt-4 daisy-pb-4">
        <TextInput
          scope-name="memory_tracker"
          field="answer"
          v-model="spellingAnswer"
          placeholder="put your answer here"
          v-focus
        >
          <template #input_append>
            <input
              type="submit"
              value="Answer"
              class="daisy-btn daisy-btn-primary daisy-btn"
            />
          </template>
        </TextInput>
      </form>
    </template>
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  onMounted,
  computed,
  onUnmounted,
  onActivated,
  onDeactivated,
} from "vue"
import type { RecallPrompt } from "@generated/backend"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import TextInput from "../form/TextInput.vue"
import QuestionStem from "./QuestionStem.vue"
import ContentLoader from "../commons/ContentLoader.vue"
import { useThinkingTimeTracker } from "@/composables/useThinkingTimeTracker"

const props = defineProps({
  memoryTrackerId: {
    type: Number,
    required: true,
  },
})

const emits = defineEmits(["answer"])
const spellingAnswer = ref("")
const recallPrompt = ref<RecallPrompt>()
const loading = ref(true)

const { start, stop, pause, resume, updateAccumulatedTime } =
  useThinkingTimeTracker()
const displayTime = ref("0.0s")
let animationFrameId: number | null = null

const updateDisplay = () => {
  const ms = updateAccumulatedTime()
  const seconds = (ms / 1000).toFixed(1)
  displayTime.value = `${seconds}s`
  animationFrameId = requestAnimationFrame(updateDisplay)
}

const stem = computed(() => {
  return recallPrompt.value?.spellingQuestion?.stem || ""
})

const fetchSpellingQuestion = async () => {
  loading.value = true
  const { data: prompt, error: promptError } =
    await MemoryTrackerController.askAQuestion({
      path: { memoryTracker: props.memoryTrackerId },
    })
  if (!promptError && prompt) {
    recallPrompt.value = prompt
  }
  loading.value = false
}

const submitAnswer = () => {
  const thinkingTimeMs = stop()
  emits("answer", {
    spellingAnswer: spellingAnswer.value,
    thinkingTimeMs,
    recallPromptId: recallPrompt.value?.id,
  })
}

onMounted(() => {
  fetchSpellingQuestion()
  start()
  updateDisplay()
})

onActivated(() => {
  resume()
  updateDisplay()
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
</script>
