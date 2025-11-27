<template>
  <div class="quiz-instruction daisy-relative daisy-mt-5" data-test="question-section">
    <ContentLoader v-if="loading" />
    <template v-else>
      <div v-if="spellingQuestion?.notebook" class="notebook-source daisy-mb-4">
        <NotebookLink :notebook="spellingQuestion.notebook" />
      </div>
      <QuestionStem :stem="spellingQuestion?.stem" />
      <form @submit.prevent="submitAnswer">
        <TextInput
          scope-name="memory_tracker"
          field="answer"
          v-model="spellingAnswer"
          placeholder="put your answer here"
          v-focus
        />
        <input
          type="submit"
          value="Answer"
          class="daisy-btn daisy-btn-primary daisy-btn-lg daisy-w-full"
        />
      </form>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { SpellingQuestion } from "@generated/backend"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"
import { globalClientSilent } from "@/managedApi/clientSetup"
import TextInput from "../form/TextInput.vue"
import QuestionStem from "./QuestionStem.vue"
import ContentLoader from "../commons/ContentLoader.vue"

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

const fetchSpellingQuestion = async () => {
  loading.value = true
  const { data: question, error } =
    await MemoryTrackerController.getSpellingQuestion({
      path: { memoryTracker: props.memoryTrackerId },
      client: globalClientSilent,
    })
  if (!error) {
    spellingQuestion.value = question!
  }
  loading.value = false
}

const submitAnswer = () => {
  emits("answer", { spellingAnswer: spellingAnswer.value })
}

onMounted(() => {
  fetchSpellingQuestion()
})
</script>
