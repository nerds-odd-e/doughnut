<template>
  <h2>Edit Suggested Question For AI Fine Tuning</h2>
  <div>
    <TextArea
      :field="`stem`"
      v-model="suggestionParams.preservedQuestion.stem"
      placeholder="Add a suggested question"
      :rows="2"
    /><br />
    <ol type="A">
      <li
        v-for="(_, index) in suggestionParams.preservedQuestion.choices"
        :key="index"
      >
        <input
          type="text"
          v-model="suggestionParams.preservedQuestion.choices[index]"
        />
      </li>
    </ol>
    <TextInput
      field="comment"
      v-model="suggestionParams.comment"
      placeholder="Add a comment about the question"
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
      suggestionParams: _.cloneDeep(
        this.modelValue,
      ) as Generated.QuestionSuggestionParams,
    };
  },
  methods: {
    async suggestQuestionForFineTuning() {
      const updated =
        await this.api.fineTuning.suggestedQuestionForFineTuningUpdate(
          this.modelValue.id,
          this.suggestionParams,
        );
      this.$emit("update:modelValue", updated);
      this.popup.done(updated);
    },
  },
  components: { TextInput, TextArea },
});
</script>
