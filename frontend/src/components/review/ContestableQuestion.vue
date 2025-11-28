<template>
  <div v-for="(q, index) in prevQuestions"
    :key="index"
    class="daisy-card daisy-shadow-sm daisy-mb-4"
  >
    <div class="daisy-card-body">
      <h3 class="daisy-card-title">Previous Question Contested ...</h3>
      <p>{{ q.badQuestionReason }}</p>
      <QuestionDisplay :multiple-choices-question="q.quizeQuestion.multipleChoicesQuestion" :disabled="true" :key="q.quizeQuestion.id"/>
    </div>
  </div>
  <p v-if="currentQuestionLegitMessage" class="daisy-text-warning daisy-mb-4">
    {{ currentQuestionLegitMessage }}
  </p>
  <ContentLoader v-if="regenerating" />
  <div class="question daisy-overflow-y-auto" v-else>
    <AnsweredQuestionComponent
      v-if="answeredQuestion"
      :answered-question="answeredQuestion"
      :conversation-button="true"
    />
    <div v-else class="daisy-flex daisy-flex-col daisy-gap-4" :class="{ 'daisy-opacity-50 daisy-pointer-events-none': contesting }">
      <RecallPromptComponent
        v-if="currentQuestion"
        :predefined-question="currentQuestion"
        @answered="onAnswered($event)"
      />
      <a
        role="button"
        title="Doesn't make sense?"
        id="try-again"
        v-if="currentQuestion"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        @click="contestQuestion"
      >
        <SvgContest />
      </a>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AnsweredQuestion, PredefinedQuestion } from "@generated/backend"
import { RecallPromptController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { ref } from "vue"
import AnsweredQuestionComponent from "./AnsweredQuestionComponent.vue"
import RecallPromptComponent from "./RecallPromptComponent.vue"
import QuestionDisplay from "./QuestionDisplay.vue"
const props = defineProps({
  predefinedQuestion: {
    type: Object as PropType<PredefinedQuestion>,
    required: true,
  },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
const emit = defineEmits(["need-scroll", "answered"])
const regenerating = ref(false)
const contesting = ref(false)
const currentQuestionLegitMessage = ref<string | undefined>(undefined)
const currentQuestion = ref(props.predefinedQuestion)
const answeredQuestion = ref<AnsweredQuestion | undefined>(undefined)
const prevQuestions = ref<
  {
    quizeQuestion: PredefinedQuestion
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
          path: { predefinedQuestion: currentQuestion.value.id },
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
              path: { predefinedQuestion: currentQuestion.value.id },
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

const onAnswered = (answer: AnsweredQuestion) => {
  answeredQuestion.value = answer
  emit("answered", answeredQuestion.value)
}
</script>

<style lang="scss" scoped>
.question {
  overflow-y: auto;
}

.notebook-source {
  margin-bottom: 1rem;
}

/* These styles are to-be replaced by DaisyUI classes:
.question -> daisy-overflow-y-auto
.notebook-source -> daisy-mb-4
*/
</style>
