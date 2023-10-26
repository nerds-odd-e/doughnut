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
      v-model="suggestionParams.preservedQuestion.stem"
      placeholder="Add a suggested question"
      :rows="2"
    /><br />
    <ul>
      <li v-for="index in [0, 1, 2, 3]" :key="index">
        <TextInput
          :field="`choice-${index}`"
          v-model="suggestionParams.preservedQuestion.choices[index]"
          :errors="errors.preservedQuestion.choices[index]"
        />
      </li>
    </ul>
    <TextInput
      field="correctChoiceIndex"
      v-model="suggestionParams.preservedQuestion.correctChoiceIndex"
      placeholder="correct choice index"
      :errors="errors.preservedQuestion.correctChoiceIndex"
    />
    <TextInput
      field="comment"
      v-model="suggestionParams.comment"
      placeholder="Add a comment about the question"
    />
    <CheckInput
      scope-name="positiveFeedback"
      v-model="suggestionParams.positiveFeedback"
    />
  </div>
  <button class="btn btn-success" @click="suggestQuestionForFineTuning">
    Save
  </button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import _ from "lodash";
import useLoadingApi from "../../managedApi/useLoadingApi";
import asPopup from "../commons/Popups/asPopup";
import TextInput from "../form/TextInput.vue";
import TextArea from "../form/TextArea.vue";
import CheckInput from "../form/CheckInput.vue";

export default defineComponent({
  inheritAttrs: false,
  setup() {
    return { ...useLoadingApi(), ...asPopup() };
  },
  props: {
    modelValue: {
      type: Object as PropType<Generated.SuggestedQuestionForFineTuning>,
      required: true,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      suggestionParams: <Generated.QuestionSuggestionParams>(
        _.cloneDeep(this.modelValue)
      ),
      errors: {
        preservedQuestion: {
          stem: "",
          choices: ["", "", "", ""] as string[],
          correctChoiceIndex: "",
        },
      },
    };
  },
  methods: {
    async suggestQuestionForFineTuning() {
      const validated = this.validateSuggestedQuestion(this.suggestionParams);
      if (!validated) return;
      const updated =
        await this.api.fineTuning.suggestedQuestionForFineTuningUpdate(
          this.modelValue.id,
          validated,
        );
      this.$emit("update:modelValue", updated);
      this.popup.done(updated);
    },
    validateSuggestedQuestion(
      params: Generated.QuestionSuggestionParams,
    ): Generated.QuestionSuggestionParams | undefined {
      const validated = _.cloneDeep(params);
      validated.preservedQuestion.choices = validated.preservedQuestion.choices
        .map((choice) => choice?.trim())
        .filter((choice) => choice?.length > 0);
      if (validated.preservedQuestion.choices.length < 2) {
        this.errors.preservedQuestion.choices[1] =
          "At least 2 choices are required";
        return undefined;
      }
      if (
        validated.preservedQuestion.choices.length <=
        validated.preservedQuestion.correctChoiceIndex
      ) {
        this.errors.preservedQuestion.correctChoiceIndex =
          "Correct choice index is out of range";
        return undefined;
      }
      return validated;
    },
  },
  components: { TextInput, TextArea, CheckInput },
});
</script>
