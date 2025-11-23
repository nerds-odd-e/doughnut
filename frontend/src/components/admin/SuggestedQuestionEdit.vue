<template>
  <h2>Edit Suggested Question For AI Fine Tuning</h2>
  <div>
    <TextArea
      :field="`preservedNoteContent`"
      v-model="suggestionParams.preservedNoteContent"
      :rows="2"
    />
    <TextArea
      :field="`stem`"
      v-model="suggestionParams.preservedQuestion.multipleChoicesQuestion.stem"
      placeholder="Add a suggested question"
      :rows="2"
    /><br />
    <ul>
      <li v-for="index in [0, 1, 2, 3]" :key="index">
        <TextInput
          :field="`choice-${index}`"
          v-model="
            suggestionParams.preservedQuestion.multipleChoicesQuestion.choices[
              index
            ]
          "
          :error-message="errors.preservedQuestion.choices[index]"
        />
      </li>
    </ul>
    <TextInput
      field="correctChoiceIndex"
      v-model="suggestionParams.preservedQuestion.correctChoiceIndex"
      placeholder="correct choice index"
      :error-message="errors.preservedQuestion.correctChoiceIndex"
    />
    <TextInput
      field="realCorrectAnswers"
      v-if="!suggestionParams.positiveFeedback"
      v-model="suggestionParams.realCorrectAnswers"
      hint="The real correct answers, separated by comma. Leave empty if there's no real correct answer."
      :error-message="errors.realCorrectAnswers"
    />
    <CheckInput
      field="positiveFeedback"
      v-model="suggestionParams.positiveFeedback"
    />
    <TextInput
      field="comment"
      v-model="suggestionParams.comment"
      placeholder="Add a comment about the question"
    />
  </div>
  <button class="daisy-btn daisy-btn-success" @click="suggestQuestionForFineTuning">
    Save
  </button>
</template>

<script lang="ts">
import type { PropType } from "vue"
import { defineComponent } from "vue"
import { cloneDeep } from "es-toolkit"
import type {
  QuestionSuggestionParams,
  SuggestedQuestionForFineTuning,
} from "@generated/backend"
import { updateSuggestedQuestionForFineTuning } from "@generated/backend/sdk.gen"
import CheckInput from "../form/CheckInput.vue"
import TextArea from "../form/TextArea.vue"
import TextInput from "../form/TextInput.vue"

const validateRealCorrectAnswers = (answers: string) => {
  if (answers.length === 0) return true
  const numbers = answers.split(",")
  return numbers.every((number) => Number.isInteger(Number(number)))
}

export default defineComponent({
  inheritAttrs: false,
  props: {
    modelValue: {
      type: Object as PropType<SuggestedQuestionForFineTuning>,
      required: true,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      suggestionParams: <QuestionSuggestionParams>cloneDeep(this.modelValue),
      errors: {
        preservedQuestion: {
          stem: "",
          choices: ["", "", "", ""] as string[],
          correctChoiceIndex: "",
        },
        realCorrectAnswers: "",
      },
    }
  },
  methods: {
    async suggestQuestionForFineTuning() {
      const validated = this.validateSuggestedQuestion(this.suggestionParams)
      if (!validated) return
      const { data: updated, error } =
        await updateSuggestedQuestionForFineTuning({
          path: { suggestedQuestion: this.modelValue.id },
          body: validated,
        })
      if (!error && updated) {
        this.$emit("update:modelValue", updated)
      }
    },
    validateSuggestedQuestion(
      params: QuestionSuggestionParams
    ): QuestionSuggestionParams | undefined {
      const validated = cloneDeep(params)
      validated.preservedQuestion.multipleChoicesQuestion.choices =
        validated.preservedQuestion.multipleChoicesQuestion.choices
          .map((choice) => choice?.trim())
          .filter((choice) => choice?.length > 0)
      if (
        validated.preservedQuestion.multipleChoicesQuestion.choices.length < 2
      ) {
        this.errors.preservedQuestion.choices[1] =
          "At least 2 choices are required"
        return undefined
      }
      if (
        validated.preservedQuestion.multipleChoicesQuestion.choices.length <=
        validated.preservedQuestion.correctChoiceIndex
      ) {
        this.errors.preservedQuestion.correctChoiceIndex =
          "Correct choice index is out of range"
        return undefined
      }
      if (!validateRealCorrectAnswers(validated.realCorrectAnswers)) {
        this.errors.realCorrectAnswers = "must be a number list"
        return undefined
      }
      return validated
    },
  },
  components: { TextInput, TextArea, CheckInput },
})
</script>
