<template>
  <h2>Suggest This Question For AI Fine Tuning</h2>
  <p>
    <i
      >Sending this question for fine tuning the question generation model will
      make this note and question visible to admin. Are you sure?</i
    >
  </p>
  <div>
    <TextArea
      :field="`stem`"
      v-model="suggestedQuestionStem"
      placeholder="Add a suggested question"
      :rows="2"
    /><br />
    <ol type="A">
      <li v-for="choice in originalChoices" :key="choice">
        {{ choice }}
      </li>
    </ol>
    <TextInput
      field="comment"
      v-model="comment"
      placeholder="Add a comment about the question"
    />
  </div>
  <button class="btn btn-success" @click="suggestQuestionForFineTuning">
    OK
  </button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
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
    quizQuestion: {
      type: Object as PropType<Generated.QuizQuestion>,
      required: true,
    },
  },
  data() {
    return {
      suggestedQuestionStem: this.quizQuestion.stem as string,
      comment: "" as string,
    };
  },
  computed: {
    originalChoices(): string[] {
      return this.quizQuestion.choices.map((c) => c.display);
    },
  },
  methods: {
    async suggestQuestionForFineTuning() {
      await this.api.reviewMethods.suggestQuestionForFineTuning(
        this.quizQuestion.quizQuestionId,
        {
          comment: this.comment,
          preservedQuestion: {
            stem: this.suggestedQuestionStem,
            correctChoiceIndex: 0,
            choices: this.originalChoices,
            confidence: 9,
          },
        },
      );
      this.popup.done(null);
    },
  },
  components: { TextInput, TextArea },
});
</script>
