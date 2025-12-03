<template>
  <div class="quiz-instruction daisy-relative daisy-h-full daisy-flex daisy-flex-col" data-test="question-section">
    <ContentLoader v-if="loading" />
    <template v-else>
      <div class="daisy-flex-1 daisy-overflow-y-auto daisy-pb-4">
        <div v-if="recallPrompt?.notebook" class="notebook-source daisy-mb-4">
          <NotebookLink :notebook="recallPrompt.notebook" />
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
import type { RecallPrompt, MemoryTracker } from "@generated/backend"
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
const memoryTracker = ref<MemoryTracker>()
const loading = ref(true)

const { start, stop } = useThinkingTimeTracker()

const stem = computed(() => {
  if (!memoryTracker.value?.note) return ""
  // @ts-expect-error - clozeDescription is a method on Note, not a property
  return memoryTracker.value.note.clozeDescription?.clozeDetails?.() || ""
})

const fetchSpellingQuestion = async () => {
  loading.value = true
  const { data: prompt, error: promptError } =
    await MemoryTrackerController.getSpellingQuestion({
      path: { memoryTracker: props.memoryTrackerId },
    })
  if (!promptError && prompt) {
    recallPrompt.value = prompt
    const { data: tracker } = await MemoryTrackerController.showMemoryTracker({
      path: { memoryTracker: props.memoryTrackerId },
    })
    if (tracker) {
      memoryTracker.value = tracker
    }
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
})
</script>
