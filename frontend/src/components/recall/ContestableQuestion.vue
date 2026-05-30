<template>
  <div v-for="(q, index) in prevQuestions"
    :key="index"
    class="daisy-card shadow-sm mb-4"
  >
    <div class="daisy-card-body">
      <h3 class="daisy-card-title">Previous Question Contested ...</h3>
      <p>{{ q.badQuestionReason }}</p>
      <QuestionDisplay v-if="q.quizeQuestion.multipleChoicesQuestion" :multiple-choices-question="q.quizeQuestion.multipleChoicesQuestion" :disabled="true" :key="q.quizeQuestion.id"/>
    </div>
  </div>
  <p v-if="currentQuestionLegitMessage" class="text-warning mb-4">
    {{ currentQuestionLegitMessage }}
  </p>
  <ContentLoader v-if="regenerating" />
  <div class="recall-prompt overflow-y-auto" v-else>
    <div class="flex flex-col gap-4" :class="{ 'opacity-50 pointer-events-none': contesting }">
      <RecallPromptComponent
        v-if="currentQuestion"
        :recall-prompt="currentQuestion"
        :next-is-spelling="nextIsSpelling"
        @answered="onAnswered($event)"
      />
      <button
        type="button"
        aria-label="Doesn't make sense?"
        title="Doesn't make sense?"
        id="try-again"
        v-if="currentQuestion"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        @click="contestQuestion"
      >
        <Target class="w-6 h-6" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RecallPrompt } from "@generated/doughnut-backend-api"
import { RecallPromptController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { PropType } from "vue"
import { ref } from "vue"
import { Target } from "@lucide/vue"
import RecallPromptComponent from "./RecallPromptComponent.vue"
import QuestionDisplay from "./QuestionDisplay.vue"
const props = defineProps({
  recallPrompt: {
    type: Object as PropType<RecallPrompt>,
    required: true,
  },
  nextIsSpelling: {
    type: Boolean,
    default: false,
  },
})
const emit = defineEmits<{
  (e: "need-scroll"): void
  (e: "answered", result: RecallPrompt): void
}>()
const regenerating = ref(false)
const contesting = ref(false)
const currentQuestionLegitMessage = ref<string | undefined>(undefined)
const currentQuestion = ref(props.recallPrompt)
const prevQuestions = ref<
  {
    quizeQuestion: RecallPrompt
    badQuestionReason: string | undefined
  }[]
>([])

const scrollToBottom = () => {
  emit("need-scroll")
}

const contestQuestion = async () => {
  currentQuestionLegitMessage.value = ""
  contesting.value = true
  try {
    const { data: contestResult, error: contestError } =
      await apiCallWithLoading(() =>
        RecallPromptController.contest({
          path: { recallPrompt: currentQuestion.value.id },
        })
      )

    if (!contestError && contestResult && !contestResult.rejected) {
      regenerating.value = true
      prevQuestions.value.push({
        quizeQuestion: currentQuestion.value,
        badQuestionReason: contestResult.advice,
      })
      try {
        const { data: regeneratedQuestion, error: regenerateError } =
          await apiCallWithLoading(() =>
            RecallPromptController.regenerate({
              path: { recallPrompt: currentQuestion.value.id },
              body: contestResult,
            })
          )
        if (!regenerateError && regeneratedQuestion) {
          currentQuestion.value = regeneratedQuestion
        }
      } finally {
        regenerating.value = false
      }
    } else if (contestResult) {
      currentQuestionLegitMessage.value = contestResult.advice
    }
  } finally {
    contesting.value = false
    scrollToBottom()
  }
}

const onAnswered = (answer: RecallPrompt) => {
  emit("answered", answer)
}
</script>

<style lang="scss" scoped>
.recall-prompt {
  overflow-y: auto;
}

.notebook-source {
  margin-bottom: 1rem;
}

/* These styles are to-be replaced by DaisyUI classes:
.recall-prompt -> overflow-y-auto
.notebook-source -> mb-4
*/
</style>
