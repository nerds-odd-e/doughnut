<template>
  <div v-if="recallPrompt.notebook" class="notebook-source">
    From notebook: <NotebookLink :notebook="recallPrompt.notebook" />
  </div>
  <div v-for="(q, index) in prevQuestions" :key="index">
    <h3>Previous Question Contested ...</h3>
    <p>{{ q.badQuestionReason }}</p>
    <QuestionDisplay :bare-question="q.quizeQuestion.bareQuestion" :disabled="true" />
  </div>
  <p v-if="currentQuestionLegitMessage">{{ currentQuestionLegitMessage }}</p>
  <ContentLoader v-if="regenerating" />
  <div class="recall-prompt" v-else>
    <AnsweredQuestionComponent
      v-if="answeredQuestion"
      :answered-question="answeredQuestion"
      :conversation-button="true"
      :storage-accessor="storageAccessor"
    />
    <div v-else>
    <RecallPromptComponent
      :recall-prompt="currentQuestion"
      @answered="onAnswered($event)"
    />
      <a
        role="button"
        title="Doesn't make sense?"
        id="try-again"
        v-if="currentQuestion"
        class="btn"
        @click="contest"
      >
        <SvgContest />
      </a>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AnsweredQuestion, RecallPrompt } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { ref } from "vue"
import NotebookLink from "../notes/NotebookLink.vue"
import AnsweredQuestionComponent from "./AnsweredQuestionComponent.vue"
import RecallPromptComponent from "./RecallPromptComponent.vue"
import QuestionDisplay from "./QuestionDisplay.vue"

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
  const contestResult = await managedApi.restRecallPromptController.contest(
    currentQuestion.value.id
  )

  if (!contestResult.rejected) {
    regenerating.value = true
    prevQuestions.value.push({
      quizeQuestion: currentQuestion.value,
      badQuestionReason: contestResult.reason,
    })
    currentQuestion.value =
      await managedApi.restRecallPromptController.regenerate(
        currentQuestion.value.id
      )
  } else {
    currentQuestionLegitMessage.value = contestResult.reason
  }
  regenerating.value = false
  scrollToBottom()
}

const onAnswered = (answer: AnsweredQuestion) => {
  answeredQuestion.value = answer
  emit("answered", answeredQuestion.value)
}
</script>

<style lang="scss" scoped>
.recall-prompt {
  overflow-y: auto;
}
.notebook-source {
  margin-bottom: 1rem;
}
</style>
