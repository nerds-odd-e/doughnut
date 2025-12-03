<template>
  <div class="quiz-instruction daisy-relative daisy-h-full daisy-flex daisy-flex-col" data-test="question-section">
    <ContentLoader v-if="loading" />
    <template v-else>
      <div class="daisy-flex-1 daisy-overflow-y-auto daisy-pb-4">
        <div v-if="spellingQuestion?.notebook" class="notebook-source daisy-mb-4">
          <NotebookLink :notebook="spellingQuestion.notebook" />
        </div>
        <QuestionStem :stem="spellingQuestion?.stem" />
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
import { ref, onMounted } from "vue"
import type { SpellingQuestion } from "@generated/backend"
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
const spellingQuestion = ref<SpellingQuestion>()
const loading = ref(true)

const { start, stop } = useThinkingTimeTracker()

const fetchSpellingQuestion = async () => {
  loading.value = true
  const { data: question, error } =
    await MemoryTrackerController.getSpellingQuestion({
      path: { memoryTracker: props.memoryTrackerId },
    })
  if (!error) {
    spellingQuestion.value = question!
  }
  loading.value = false
}

const submitAnswer = () => {
  const thinkingTimeMs = stop()
  emits("answer", { spellingAnswer: spellingAnswer.value, thinkingTimeMs })
}

onMounted(() => {
  fetchSpellingQuestion()
  start()
})
</script>
