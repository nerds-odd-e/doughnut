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
import TextInput from "../form/TextInput.vue"
import QuestionStem from "./QuestionStem.vue"
import ContentLoader from "../commons/ContentLoader.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"

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
const { managedApi } = useLoadingApi()

const fetchSpellingQuestion = async () => {
  try {
    loading.value = true
    spellingQuestion.value = await managedApi.services.getSpellingQuestion({
      memoryTracker: props.memoryTrackerId,
    })
  } catch (e) {
    // Error handling is already done by managedApi
  } finally {
    loading.value = false
  }
}

const submitAnswer = () => {
  emits("answer", { spellingAnswer: spellingAnswer.value })
}

onMounted(() => {
  fetchSpellingQuestion()
})
</script>
