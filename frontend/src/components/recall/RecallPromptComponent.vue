<template>
  <div class="relative">
    <QuestionDisplay
      v-if="recallPrompt.mcq"
      v-bind="{
        questionStem: recallPrompt.mcq.questionStem,
        responseChoices: recallPrompt.mcq.responseChoices,
      }"
      @answer="submitAnswer($event)"
      :key="recallPrompt.id"
      :disabled="phase !== 'ready'"
      :selected-choice-index="submission?.choiceIndex"
    />

    <div v-if="phase === 'pending' || phase === 'answered'" data-test="answered-overlay" class="absolute inset-0 bg-base-100/80 z-50">
      <div v-if="phase === 'pending'" class="flex justify-center items-center h-full">
        <span class="daisy-loading daisy-loading-spinner daisy-loading-lg"></span>
      </div>
    </div>

    <div v-if="phase === 'uncertain'" class="daisy-alert daisy-alert-error mt-4" data-testid="answer-recovery">
      <span>We couldn't confirm your answer. Retry to recover this attempt.</span>
      <button type="button" class="daisy-btn daisy-btn-sm" data-testid="retry-answer" @click="retryAnswer">Retry</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, shallowRef, watch } from "vue"
import type { PropType } from "vue"
import type {
  AnsweredQuestion,
  AnswerDto,
  RecallPrompt,
} from "@generated/donut-backend-api"
import { RecallPromptController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import QuestionDisplay from "./QuestionDisplay.vue"
import { primeSoftKeyboard } from "@/utils/focusTarget"

const phase = ref<"ready" | "pending" | "uncertain" | "answered">("ready")
const submission = shallowRef<AnswerDto>()
let mounted = true
onBeforeUnmount(() => {
  mounted = false
})

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

const emits = defineEmits<{
  (e: "answered", result: AnsweredQuestion): void
}>()

watch(
  () => props.recallPrompt.id,
  () => {
    submission.value = undefined
    phase.value = "ready"
  }
)

const sendSubmission = async () => {
  if (props.nextIsSpelling) primeSoftKeyboard()
  const promptId = props.recallPrompt.id
  const body = submission.value!
  phase.value = "pending"
  const { data: answerResult, error: apiError } = await apiCallWithLoading(() =>
    RecallPromptController.answer({
      path: { recallPrompt: promptId },
      body,
    })
  )
  if (!mounted || props.recallPrompt.id !== promptId) return
  if (apiError) {
    phase.value = "uncertain"
  } else {
    phase.value = "answered"
    emits("answered", answerResult!)
  }
}

const submitAnswer = async (answerData: AnswerDto) => {
  if (phase.value !== "ready" || answerData.choiceIndex === undefined) return
  submission.value = Object.freeze({ ...answerData })
  await sendSubmission()
}

const retryAnswer = async () => {
  if (phase.value !== "uncertain") return
  await sendSubmission()
}
</script>
