<template>
  <div class="quiz-instruction daisy-relative daisy-h-full daisy-flex daisy-flex-col" data-test="question-section">
    <ContentLoader v-if="loading" />
    <template v-else>
      <div class="daisy-flex-1 daisy-overflow-y-auto daisy-pb-4">
        <div v-if="recallPrompt?.spellingQuestion?.notebook" class="notebook-source daisy-mb-4">
          <NotebookLink :notebook="recallPrompt.spellingQuestion.notebook" />
        </div>
        <QuestionStem :stem="stem" />
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
import { ref, onMounted, computed } from "vue"
import type { RecallPrompt } from "@generated/backend"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"
import TextInput from "../form/TextInput.vue"
import QuestionStem from "./QuestionStem.vue"
import ContentLoader from "../commons/ContentLoader.vue"
import { useQuestionThinkingTime } from "@/composables/useQuestionThinkingTime"

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

const isActiveQuestion = computed(() => !loading.value)
const { stop } = useQuestionThinkingTime(isActiveQuestion)

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
