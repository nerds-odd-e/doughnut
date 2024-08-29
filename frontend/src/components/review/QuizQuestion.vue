<template>
  <div class="quiz-instruction" data-test="question-section" :key="quizQuestion.id">
    <ShowImage v-if="quizQuestion.imageWithMask" v-bind="quizQuestion.imageWithMask" :opacity="1" />
    <div style="white-space: pre-wrap" data-test="stem" v-if="quizQuestion.multipleChoicesQuestion.stem"
      v-html="quizQuestion.multipleChoicesQuestion.stem"></div>

    <div v-if="
      !quizQuestion.multipleChoicesQuestion.choices ||
      quizQuestion.multipleChoicesQuestion.choices.length === 0
    ">
      <form @submit.prevent.once="submitAnswer({ spellingAnswer: answer })">
        <TextInput scope-name="review_point" field="answer" v-model="answer" placeholder="put your answer here"
          v-focus />
        <input type="submit" value="OK" class="btn btn-primary btn-lg btn-block" />
      </form>
    </div>
    <QuizQuestionChoices v-if="quizQuestion.multipleChoicesQuestion.choices"
      :choices="quizQuestion.multipleChoicesQuestion.choices" :correct-choice-index="correctChoiceIndex"
      :answer-choice-index="answerChoiceIndex" :disabled="disabled" @answer="submitAnswer($event)" :assessment-current-choice-index="checkAssessmentAnsweredIndex()" />
    <div class="mark-question">
      <PopButton title="send this question for fine tuning the question generation model" v-if="showFinetuneButton">
        <template #button_face>
          <SvgRaiseHand />
        </template>
        <template #default="{ closer }">
          <SuggestQuestionForFineTuning :quiz-question="quizQuestion" @close-dialog="closer()" />
        </template>
      </PopButton>
      <slot />
    </div>
  </div>
</template>

<script lang="ts">
import { AnswerDTO, QuizQuestion } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { PropType, defineComponent } from "vue"
import usePopups from "../commons/Popups/usePopups"
import TextInput from "../form/TextInput.vue"
import ShowImage from "../notes/accessory/ShowImage.vue"
import SvgRaiseHand from "../svgs/SvgRaiseHand.vue"
import QuizQuestionChoices from "./QuizQuestionChoices.vue"
import FeedbackForm from "./FeedbackForm.vue"

export default defineComponent({
  inheritAttrs: false,
  setup() {
    return { ...useLoadingApi(), ...usePopups() }
  },
  props: {
    quizQuestion: {
      type: Object as PropType<QuizQuestion>,
      required: true,
    },
    correctChoiceIndex: Number,
    answerChoiceIndex: Number,
    disabled: Boolean,
    showFinetuneButton: {
      type: Boolean,
      default: true,
    },
  },
  components: {
    ShowImage,
    TextInput,
    QuizQuestionChoices,
    SvgRaiseHand,
    FeedbackForm,
  },
  emits: ["answered"],
  data() {
    return {
      answer: "" as string,
      assessmentAnsweredIndex: 1 as number,
    }
  },
  methods: {
    async submitAnswer(answerData: AnswerDTO) {
      try {
        const answerResult =
          await this.managedApi.restQuizQuestionController.answerQuiz(
            this.quizQuestion.id,
            answerData
          )

        if (typeof answerData.choiceIndex === "number") {
          this.assessmentAnsweredIndex = answerData.choiceIndex
        }

        this.$emit("answered", answerResult)
      } catch (_e) {
        await this.popups.alert(
          "This review point doesn't exist any more or is being skipped now. Moving on to the next review point..."
        )
      }
    },
    checkAssessmentAnsweredIndex() {
      return this.assessmentAnsweredIndex
    },
  },
})
</script>

<style lang="scss" scoped>
.quiz-instruction {
  position: relative;
  margin-top: 20px;
}

.mark-question {
  button {
    border: none;
    background: none;
  }
}
</style>
