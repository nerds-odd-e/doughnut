<template>
  <div class="spelling-question-container" data-test="question-section">
    <ContentLoader v-if="loading" />
    <template v-else>
      <div class="spelling-prompt-container">
        <div v-if="spellingQuestion?.notebook" class="notebook-source daisy-mb-4">
          <NotebookLink :notebook="spellingQuestion.notebook" />
        </div>
        <QuestionStem :stem="spellingQuestion?.stem" />
      </div>
      <form class="spelling-form" @submit.prevent="submitAnswer">
        <div class="daisy-join daisy-w-full">
          <TextInput
            scope-name="memory_tracker"
            field="answer"
            v-model="spellingAnswer"
            placeholder="put your answer here"
            v-focus
            class="daisy-join-item daisy-flex-1"
          />
          <input
            type="submit"
            value="Answer"
            class="daisy-btn daisy-btn-primary daisy-btn-lg daisy-join-item"
          />
        </div>
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

<style lang="scss" scoped>
.spelling-question-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.spelling-prompt-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0;
  padding-bottom: 1rem;
}

.spelling-form {
  flex-shrink: 0;
  padding: 1rem;
  background: hsl(var(--b1));
  border-top: 1px solid hsl(var(--bc) / 0.2);
}
</style>
