<template>
  <div v-if="recallPrompt.notebook" class="notebook-source daisy-mb-4">
    <NotebookLink :notebook="recallPrompt.notebook" />
  </div>
  <div v-for="(q, index) in prevQuestions"
    :key="index"
    class="daisy-card daisy-shadow-sm daisy-mb-4"
  >
    <div class="daisy-card-body">
      <h3 class="daisy-card-title">Previous Question Contested ...</h3>
      <p>{{ q.badQuestionReason }}</p>
      <QuestionDisplay :bare-question="q.quizeQuestion.bareQuestion" :disabled="true" :key="q.quizeQuestion.id"/>
    </div>
  </div>
  <p v-if="currentQuestionLegitMessage" class="daisy-text-warning daisy-mb-4">
    {{ currentQuestionLegitMessage }}
  </p>
  <ContentLoader v-if="regenerating" />
  <div class="recall-prompt daisy-overflow-y-auto" v-else>
    <AnsweredQuestionComponent
      v-if="answeredQuestion"
      :answered-question="answeredQuestion"
      :conversation-button="true"
    />
    <div v-else class="daisy-flex daisy-flex-col daisy-gap-4" :class="{ 'daisy-opacity-50 daisy-pointer-events-none': contesting }">
      <RecallPromptComponent
        v-if="currentQuestion && currentQuestion.bareQuestion.multipleChoicesQuestion.choices && currentQuestion.bareQuestion.multipleChoicesQuestion.choices.length > 0"
        :recall-prompt="currentQuestion"
        @answered="onAnswered($event)"
      />
      <SpellingQuestionDisplay
        v-else-if="currentQuestion"
        v-bind="{
          bareQuestion: currentQuestion.bareQuestion,
        }"
        @answer="onSpellingAnswered($event)"
        :key="`spelling-${currentQuestion.id}`"
      />
      <a
        role="button"
        title="Doesn't make sense?"
        id="try-again"
        v-if="currentQuestion"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        @click="contest"
      >
        <SvgContest />
      </a>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {
  AnsweredQuestion,
  RecallPrompt,
  AnswerSpellingDTO,
} from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { ref } from "vue"
import NotebookLink from "../notes/NotebookLink.vue"
import AnsweredQuestionComponent from "./AnsweredQuestionComponent.vue"
import RecallPromptComponent from "./RecallPromptComponent.vue"
import QuestionDisplay from "./QuestionDisplay.vue"
import SpellingQuestionDisplay from "./SpellingQuestionDisplay.vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  recallPrompt: {
    type: Object as PropType<RecallPrompt>,
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
const currentQuestion = ref(props.recallPrompt)
const answeredQuestion = ref<AnsweredQuestion | undefined>(undefined)
const prevQuestions = ref<
  {
    quizeQuestion: RecallPrompt
    badQuestionReason: string | undefined
  }[]
>([])

const scrollToBottom = () => {
  emit("need-scroll")
}

const contest = async () => {
  currentQuestionLegitMessage.value = ""
  contesting.value = true
  try {
    const contestResult = await managedApi.restRecallPromptController.contest(
      currentQuestion.value.id
    )

    if (!contestResult.rejected) {
      regenerating.value = true
      prevQuestions.value.push({
        quizeQuestion: currentQuestion.value,
        badQuestionReason: contestResult.reason,
      })
      try {
        currentQuestion.value =
          await managedApi.restRecallPromptController.regenerate(
            currentQuestion.value.id,
            contestResult
          )
      } finally {
        regenerating.value = false
      }
    } else {
      currentQuestionLegitMessage.value = contestResult.reason
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

const onSpellingAnswered = async (answerData: AnswerSpellingDTO) => {
  if (answerData.spellingAnswer === undefined) return

  try {
    const answerResult =
      await managedApi.restRecallPromptController.answerSpelling(
        currentQuestion.value.id,
        { spellingAnswer: answerData.spellingAnswer }
      )
    answeredQuestion.value = answerResult
    emit("answered", answerResult)
  } catch (e) {
    // Error handling is already done in the component
  }
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
.recall-prompt -> daisy-overflow-y-auto
.notebook-source -> daisy-mb-4
*/
</style>
