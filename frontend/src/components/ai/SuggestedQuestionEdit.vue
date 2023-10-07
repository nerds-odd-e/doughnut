<template>
  <h2>Suggested Question For AI Fine Tuning</h2>
  <p>
    <i
      >Sending this question for fine tuning the question generation model will
      make this note and question visible to admin. Are you sure?</i
    >
  </p>
  <div>
    <TextArea
      :field="`stem`"
      v-model="suggestionParams.preservedQuestion.stem"
      placeholder="Add a suggested question"
      :rows="2"
    /><br />
    <ol type="A">
      <li
        v-for="choice in suggestionParams.preservedQuestion.choices"
        :key="choice"
      >
        {{ choice }}
      </li>
    </ol>
    <TextInput
      field="comment"
      v-model="suggestionParams.comment"
      placeholder="Add a comment about the question"
    />
  </div>
  <button class="btn btn-success" @click="suggestQuestionForFineTuning">
    OK
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
    suggestedQuestion: {
      type: Object as PropType<Generated.SuggestedQuestionForFineTuning>,
      required: true,
    },
  },
  data() {
    return {
      suggestionParams: _.cloneDeep(
        this.suggestedQuestion,
      ) as Generated.QuestionSuggestionParams,
    };
  },
  methods: {
    async suggestQuestionForFineTuning() {
      await this.api.reviewMethods.suggestedQuestionForFineTuningUpdate(
        this.suggestedQuestion.id,
        this.suggestionParams,
      );
      this.popup.done(null);
    },
  },
  components: { TextInput, TextArea },
});
</script>
