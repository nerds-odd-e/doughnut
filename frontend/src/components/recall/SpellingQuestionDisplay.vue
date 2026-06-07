<template>
  <div class="quiz-instruction relative h-full flex flex-col" data-test="question-section">
    <InactiveRecallMask :show="isActiveQuestion && isPaused" />
    <ContentLoader v-if="loading" />
    <template v-else>
      <div class="flex-1 overflow-y-auto pb-4">
        <div v-if="recallPrompt?.spellingQuestion?.notebook" class="notebook-source mb-4">
          <NotebookLink :notebook="recallPrompt.spellingQuestion.notebook" />
        </div>
        <QuestionStem :stem="stem" />
      </div>
      <form @submit.prevent="submitAnswer" class="sticky bottom-0 bg-base-100 pt-4 pb-4">
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
              :disabled="submitted"
            />
          </template>
        </TextInput>
      </form>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue"
import type { RecallQuestion } from "@generated/doughnut-backend-api"
import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import ContentLoader from "../commons/ContentLoader.vue"
import TextInput from "../form/TextInput.vue"
import InactiveRecallMask from "./InactiveRecallMask.vue"
import QuestionStem from "./QuestionStem.vue"
import { useQuestionThinkingTime } from "@/composables/useQuestionThinkingTime"
import { primeSoftKeyboard } from "@/utils/focusTarget"

const props = defineProps({
  memoryTrackerId: {
    type: Number,
    required: true,
  },
  nextIsSpelling: {
    type: Boolean,
    default: false,
  },
})

const emits = defineEmits(["answer"])
const spellingAnswer = ref("")
const recallPrompt = ref<RecallQuestion>()
const loading = ref(true)
const submitted = ref(false)

const isActiveQuestion = computed(() => !loading.value)
const { stop, isPaused } = useQuestionThinkingTime(isActiveQuestion)

const stem = computed(() => recallPrompt.value?.spellingQuestion?.stem || "")

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
  if (submitted.value) return
  if (props.nextIsSpelling) primeSoftKeyboard()
  submitted.value = true
  const thinkingTimeMs = stop()
  emits("answer", {
    spellingAnswer: spellingAnswer.value,
    thinkingTimeMs,
    recallPromptId: recallPrompt.value?.id,
  })
}

onMounted(() => {
  fetchSpellingQuestion()
})
</script>
