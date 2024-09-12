<template>
  <QuizQuestionDisplay
    v-bind="{
      quizQuestion,
      correctChoiceIndex,
      answerChoiceIndex,
      disabled,
      answeredCurrentQuestion,
    }"
    @answer="submitAnswer($event)"
    :key="quizQuestion.id"
   />
</template>

<script lang="ts">
import { AnswerDTO, QuizQuestion } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { PropType, defineComponent } from "vue"
import usePopups from "../commons/Popups/usePopups"
import TextInput from "../form/TextInput.vue"
import ShowImage from "../notes/accessory/ShowImage.vue"
import QuizQuestionChoices from "./QuizQuestionChoices.vue"

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
    answeredCurrentQuestion: Boolean,
  },
  components: {
    ShowImage,
    TextInput,
    QuizQuestionChoices,
  },
  emits: ["answered"],
  methods: {
    async submitAnswer(answerData: AnswerDTO) {
      try {
        const answerResult =
          await this.managedApi.restQuizQuestionController.answerQuiz(
            this.quizQuestion.id,
            answerData
          )

        this.$emit("answered", answerResult)
      } catch (_e) {
        await this.popups.alert(
          "This review point doesn't exist any more or is being skipped now. Moving on to the next review point..."
        )
      }
    },
  },
})
</script>
